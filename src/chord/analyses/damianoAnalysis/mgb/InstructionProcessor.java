package chord.analyses.damianoAnalysis.mgb;



import joeq.Class.jq_Field;
import joeq.Class.jq_Member;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
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
 * This class 
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
public class InstructionProcessor {
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
		
	protected HeapProgram program;
	protected Entry entry;
	protected jq_Method method;
	private SummaryManager summaryManager;
	private EntryManager entryManager;	
	
	public InstructionProcessor(Entry e, HeapProgram p){
		program = p;
		entry = e;
		method = entry.getMethod();
		summaryManager = program.getSummaryManager();
		entryManager = program.getEntryManager();
		relShare = program.getRelShare();
		relCycle = program.getRelCycle();		
	}
    
    /**
     * This method processes a Quad object {@code q}, branching on the operator.
     * 
     * @param q The Quad to be processed.
     * @return whether new tuples have been added.
     */
	// WARNING: make sure that all cases are covered now that this is class no longer inherits from Fixpoint
    protected boolean process(Quad q) {
    	
    	Operator operator = q.getOperator();
    	if (operator instanceof ALength) {
    		Utilities.info("IGNORING ALENGTH INSTRUCTION: " + q);
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
    		Utilities.info("IGNORING BINARY INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof BoundsCheck) {
    		Utilities.info("IGNORING BOUNDSCHECK INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Branch) {
    		Utilities.info("IGNORING BRANCH INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof CheckCast) {
    		Utilities.info("IGNORING CHECKCAST INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Getfield) {
    		return processGetfield(q);
    	}
    	if (operator instanceof Getstatic) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING GETSTATIC INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Goto) {
    		Utilities.info("IGNORING GOTO INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof InstanceOf) {
    		Utilities.info("IGNORING INSTANCEOF INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof IntIfCmp) {
    		Utilities.info("IGNORING INTIFCMP INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Invoke) {
    		// calls to <init> of the Object class can be ignored
    		if (isIgnorableInvoke(q)) {
    			Utilities.info("IGNORING INVOKE INSTRUCTION: " + q);
    			return false;
    		} else {
    			return processInvokeMethod(q);
    		}
    	}
    	if (operator instanceof Jsr) {
    		Utilities.info("IGNORING JSR INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof LookupSwitch) {
    		// TO-DO: maybe the treatment of this instruction is needed
    		Utilities.info("IGNORING LOOKUPSWITCH INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof MemLoad) {
    		// TO-DO: not clear; currently unsupported
    		Utilities.info("IGNORING MEMLOAD INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof MemStore) {
    		// TO-DO: not clear; currently unsupported
    		Utilities.info("IGNORING MEMSTORE INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Monitor) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING MONITOR INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Move) {
    		if (operator instanceof MOVE_A)
    			return processMove(q);
    		else Utilities.info("IGNORING NON-REFERENCE MOVE INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof MultiNewArray) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING MULTINEWARRAY INSTRUCTION: " + q);
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
    		Utilities.info("IGNORING NULLCHECK INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Phi) {
    		return processPhi(q);
    	}
    	if (operator instanceof Putfield) {
    		// TO-DO: check if there are other subclasses to be processed  
    		if (operator instanceof PUTFIELD_A)
    			return processPutfield(q);
    		else Utilities.info("IGNORING NON-REFERENCE PUTFIELD INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Putstatic) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING PUTSTATIC INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Ret) {
    		Utilities.info("IGNORING RET INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Return) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING RETURN INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Special) {
    		// TO-DO: currently unsupported, not clear when it is used
    		Utilities.info("IGNORING SPECIAL INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof StoreCheck) {
    		Utilities.info("IGNORING STORECHECK INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof TableSwitch) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING TABLESWITCH INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Unary) {
    		// TO-DO: subclasses involving addresses and object
    		// (ADDRESS_2OBJECT, OBJECT_2ADDRESS) unsupported
    		Utilities.info("IGNORING UNARY INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof ZeroCheck) {
    		Utilities.info("IGNORING ZEROCHECK INSTRUCTION: " + q);
    		return false;
    	}
    	// This should never happen
    	Utilities.warn("CANNOT DEAL WITH QUAD" + q);
    	return false;
    }

    /**
     * This method simply copies the information about the array variable
     * into the destination, unless the latter has primitive type.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processALoad(Quad q) {
    	Utilities.begin("PROCESSING ALOAD INSTRUCTION: " + q);
    	if (((RegisterOperand) ALoad.getDest(q)).getType().isPrimitiveType())
    		return false;
    	Register base = ((RegisterOperand) ALoad.getBase(q)).getRegister();
    	Register dest = ((RegisterOperand) ALoad.getDest(q)).getRegister();
    	boolean b = (relShare.copyTuples(entry,base,dest) | relCycle.copyTuples(entry,base,dest));
    	Utilities.end("PROCESSING ALOAD INSTRUCTION: " + q);
    	return b;
    }
    
    /**
     * This method simply copies the information about the value into the array
     * variable, unless the value has primitive type.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processAStore(Quad q) {
    	Utilities.begin("PROCESSING ASTORE INSTRUCTION: " + q);
    	if (((RegisterOperand) AStore.getValue(q)).getType().isPrimitiveType())
    		return false;
    	Register base = ((RegisterOperand) AStore.getBase(q)).getRegister();
    	Register value = ((RegisterOperand) AStore.getValue(q)).getRegister();
    	boolean b = (relShare.moveTuples(entry,value,base) | relCycle.moveTuples(entry,value,base));
    	Utilities.end("PROCESSING ASTORE INSTRUCTION: " + q);
    	return b;
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
    	Utilities.begin("PROCESSING GETFIELD INSTRUCTION: " + q);
    	if (((RegisterOperand) Getfield.getDest(q)).getType().isPrimitiveType()) return false;
    	Register base = ((RegisterOperand) Getfield.getBase(q)).getRegister();
    	Register dest = ((RegisterOperand) Getfield.getDest(q)).getRegister();
    	jq_Field field = ((FieldOperand) Getfield.getField(q)).getField();
    	Boolean changed = false;
    	// copy cyclicity from base to dest
    	changed |= relCycle.copyTuples(entry,base,dest);
    	// copy self-"reachability" of dest from from cyclicity of base
    	changed |= relShare.copyTuplesFromCycle(entry,base,dest,relCycle);
    	// add "reachability" from the "reachability" from base, removing the field
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachingRegister(entry,base)) {
    		FieldSet fs1 = FieldSet.removeField(p.val1,field);
    		changed |= relShare.condAdd(entry,dest,p.val0,fs1,FieldSet.emptyset());
    		// the old field set is still there
    		changed |= relShare.condAdd(entry,dest,p.val0,p.val1,FieldSet.emptyset());
    	}
    	// add "reachability" from the "reachability" to base, adding the field
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(entry,base)) {
    		FieldSet fs2 = FieldSet.addField(p.val1,field);
    		changed |= relShare.condAdd(entry,p.val0,dest,fs2,FieldSet.emptyset());
    	}
    	// add "reachability" to dest and sharing between r and dest from
    	// sharing between r and base 
    	for (Trio<Register,FieldSet,FieldSet> p : relShare.findTuplesByFirstRegister(entry,base)) {
    		if (p.val1.containsOnly(field)) {
    				changed |= relShare.condAdd(entry,p.val0,dest,p.val2,FieldSet.emptyset());
    			}
    		FieldSet fs3 = FieldSet.removeField(p.val1,field);
    		changed |= relShare.condAdd(entry,base,p.val0,p.val2,fs3);
    		changed |= relShare.condAdd(entry,base,p.val0,p.val2,p.val1);
    	}
    	for (Trio<Register,FieldSet,FieldSet> p : relShare.findTuplesBySecondRegister(entry,base)) {
    		if (p.val2.containsOnly(field)) {
    				changed |= relShare.condAdd(entry,p.val0,dest,p.val1,FieldSet.emptyset());
    			}
    		FieldSet fs4 = FieldSet.removeField(p.val2,field);
    		changed |= relShare.condAdd(entry,base,p.val0,p.val1,fs4);
    		changed |= relShare.condAdd(entry,base,p.val0,p.val1,p.val2);
    	}
    	Utilities.end("PROCESSING GETFIELD INSTRUCTION: " + q);
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
    	Utilities.begin("PROCESSING NEW INSTRUCTION: " + q);
    	Register r = ((RegisterOperand) New.getDest(q)).getRegister();
    	boolean b = (relCycle.condAdd(entry,r,FieldSet.emptyset()) |
    			relShare.condAdd(entry,r,r,FieldSet.emptyset(),FieldSet.emptyset()));
    	Utilities.end("PROCESSING NEW INSTRUCTION: " + q);
    	return b;
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
    	Utilities.begin("PROCESSING NEWARRAY INSTRUCTION: " + q);
    	Register r = ((RegisterOperand) NewArray.getDest(q)).getRegister();
    	boolean b = (relCycle.condAdd(entry,r,FieldSet.emptyset()) |
    			relShare.condAdd(entry,r,r,FieldSet.emptyset(),FieldSet.emptyset()));
    	Utilities.end("PROCESSING NEWARRAY INSTRUCTION: " + q);
    	return b;
    }
    
    /**
     * This method copies all tuples about the source variable into the
     * destination variable.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processMove(Quad q) {
    	Utilities.begin("PROCESSING MOVE INSTRUCTION: " + q);
    	boolean b = false;
    	Operand op = Move.getSrc(q);
    	if (op instanceof AConstOperand) b = false;
    	if (op instanceof RegisterOperand) {
    		Register src = ((RegisterOperand) op).getRegister();
    		Register dest = ((RegisterOperand) Move.getDest(q)).getRegister();
    		
    		relShare.output();
    		relCycle.output();
    		
    		if (src.isTemp() && !dest.isTemp()) { // from a stack variable to a local variable
    			b = (relCycle.moveTuples(entry,src,dest) | relShare.moveTuples(entry,src,dest));
    		} else {
    			b = (relCycle.copyTuples(entry,src,dest) | relShare.copyTuples(entry,src,dest));
    		}

    		relShare.output();
    		relCycle.output();

    	}
    	Utilities.end("PROCESSING MOVE INSTRUCTION: " + q);
    	return b;
    }
    
    /**
     * This method copies all tuples about each of the source variables into the
     * destination variable.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processPhi(Quad q) {
    	Utilities.begin("PROCESSING PHI INSTRUCTION: " + q);
    	Register src1 = ((RegisterOperand) Phi.getSrc(q,0)).getRegister();
    	Register src2 = ((RegisterOperand) Phi.getSrc(q,1)).getRegister();
    	Register destination = ((RegisterOperand) Phi.getDest(q)).getRegister();
    	relCycle.removeTuples(entry,destination);
    	relShare.removeTuples(entry,destination);
    	boolean b = (relCycle.joinTuples(entry,src1,src2,destination) |
    			relShare.joinTuples(entry,src1,src2,destination));
    	Utilities.end("PROCESSING PHI INSTRUCTION: " + q);
    	return b;
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
    	if (Putfield.getSrc(q) instanceof AConstOperand)
    		return false;    	
    	if (((RegisterOperand) Putfield.getSrc(q)).getType().isPrimitiveType())
    		return false;
    	
    	Utilities.begin("PROCESSING PUTFIELD INSTRUCTION: " + q);
    	
    	Register base = ((RegisterOperand) Putfield.getBase(q)).getRegister();//r6
    	Register src = ((RegisterOperand) Putfield.getSrc(q)).getRegister();//r1
    	jq_Field field = ((FieldOperand) Putfield.getField(q)).getField();//left
    	Boolean changed = false;
    	// add "reachability" created by the new path
    	for (Pair<Register,FieldSet> p1 : relShare.findTuplesByReachedRegister(entry,base)) {
        	for (Pair<Register,FieldSet> p2 : relShare.findTuplesByReachingRegister(entry,src)) {
    			FieldSet fs1 = FieldSet.union(p1.val1,FieldSet.addField(p2.val1,field));//left
    			changed |= relShare.condAdd(entry,p1.val0,p2.val0,fs1,FieldSet.emptyset());//
    			changed |= relShare.condAdd(entry,p1.val0,p1.val0,fs1,fs1);
    			for (FieldSet fs2 : relShare.findTuplesByReachingReachedRegister(entry,src,base)) {
    				FieldSet fs3 = FieldSet.union(fs1,fs2);
    				changed |= relShare.condAdd(entry,p1.val0,p2.val0,fs3,FieldSet.emptyset());
    				changed |= relShare.condAdd(entry,p1.val0,p1.val0,fs3,fs3);
    			}
    		}
    	}
    	// add cyclicity of variables "reaching" base
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(entry,base)) {
    		for (FieldSet fs : relShare.findTuplesByReachingReachedRegister(entry,src,base)) {
    			FieldSet fs0 = FieldSet.addField(fs,field);
    			changed |= relCycle.condAdd(entry,p.val0,fs0);
    			changed |= relShare.condAdd(entry,p.val0,p.val0,fs0,fs0);
    		}
    	}
    	// copy cyclicity of src into variables which "reach" base
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(entry,base)) {
    		changed |= relCycle.copyTuples(entry,src,p.val0);
    		changed |= relShare.copyTuplesFromCycle(entry,src,p.val0,relCycle);
    	}
        // add sharing from sharing
    	for (Trio<Register,FieldSet,FieldSet> t : relShare.findTuplesByFirstRegister(entry,src)) {
    		FieldSet fs = FieldSet.addField(t.val1,field);
    		changed |= relShare.condAdd(entry,base,t.val0,fs,t.val2);
    	}
    	for (Trio<Register,FieldSet,FieldSet> t : relShare.findTuplesBySecondRegister(entry,src)) {
    		FieldSet fs = FieldSet.addField(t.val2,field);
    		changed |= relShare.condAdd(entry,t.val0,base,t.val1,fs);
    	}
    	Utilities.end("PROCESSING PUTFIELD INSTRUCTION: " + q);
    	return changed;
    }
    
    /**
     * This method takes an invoke Quad q and processes it. It includes: 
     * - Update the input of the called entry with the tuples of the registers which are
     *   passed as params.
     * - Update the information of the relations of the entry which processes q
     *   with the information of the output of the called entry. For this is necessary to change 
     *   the local registers of the called method to the registers of the calling method. 
     *   
     * @param q
     * @return boolean
     */
    protected boolean processInvokeMethod(Quad q){
		Utilities.begin("PROCESSING INVOKE INSTRUCTION: " + q);
    	
		// DEBUG
		relShare.output();
    	relCycle.output();

    	Entry invokedEntry;
    	try {
    		invokedEntry = entryManager.getRelevantEntry(q);
    	} catch (NoEntryException nee) { // this should never happen
			nee.printStackTrace();
			return false;
    	}
    	
    	boolean changed = false;

    	AbstractValue summaryInput = summaryManager.getSummaryInput(invokedEntry);
		ParamListOperand apl = Invoke.getParamList(q);
    	if (summaryInput == null) { // no information about the invoked method, so that the output will be empty
    		// WARNING: take the return value into acount
    		for (int i = 0; i<apl.length(); i++) {
    			Register r = apl.get(i).getRegister();
    			relShare.removeTuples(entry,r);
    			relCycle.removeTuples(entry,r);
    		}
       	} else {
        	AbstractValue summaryOutput = summaryManager.getSummaryOutput(invokedEntry);
       		AbstractValue renamedCopy = summaryOutput.getRenamedCopyList(apl,invokedEntry.getMethod());
       		
       		System.out.println("ZZZZZZZZZZ" + renamedCopy.getSComp().getTuples());
       		System.out.println("ZZZZZZZZZZ" + renamedCopy.getCComp().getTuples());
       	}
    	
    	/*
    	// COPY TUPLES OF INPUT REGISTERS OF CALLED METHOD TO THE SUMMARYMANAGER
    	AbstractValue av = new AbstractValue();
    	ArrayList<Pair<Register,FieldSet>> cycle = new ArrayList<>();
    	ArrayList<chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet>> share = new ArrayList<>();
        
    	int begin;
    	try {
    		Utilities.out("\t PARAM WORDS " + entryManager.getRelevantEntry(q).getMethod().getParamWords());
    		begin = entryManager.getRelevantEntry(q).getMethod().isStatic()? 0 : 1;
		} catch (NoEntryException e2) {
			e2.printStackTrace();
			begin = 0;
		}
    	for (int i = begin; i < q.getUsedRegisters().size(); i++) {
    		RegisterOperand r = q.getUsedRegisters().get(i);
    		if (r.getType().isPrimitiveType()) continue;
    		
    		Utilities.out("");
    		Utilities.out("\t VARIABLE AS PARAM FOR CYCLICITY " + RegisterManager.getVarFromReg(method,r.getRegister()) + " IN REGISTER " + r.getRegister());
    		
    		cycle.addAll(accumulatedTuples.getCFor(entry,r.getRegister()));
    	}
    	CTuples ctuples = new CTuples(cycle);
    	av.setCComp(ctuples);
    	
    	for (int i = begin; i < q.getUsedRegisters().size(); i++) {
    		RegisterOperand r = q.getUsedRegisters().get(i);
    		if (r.getType().isPrimitiveType()) continue;
    		for (int j = begin; j < q.getUsedRegisters().size(); j++) {
    			RegisterOperand r2 = q.getUsedRegisters().get(j);
    			if (r2.getType().isPrimitiveType()) continue;
    			Utilities.out("\t VARIABLE AS PARAM FOR SHARING " + RegisterManager.getVarFromReg(method,r.getRegister()) + "IN REGISTER " + r.getRegister());
    			share.addAll(accumulatedTuples.getSFor(entry, r.getRegister(),r2.getRegister()));
    		}
    	}
    	av.setSComp(new STuples(share));
    	
    	// UPDATE INPUT OF ENTRY
    	boolean changedprime = false;
    	try {
    		changedprime = summaryManager.updateSummaryInput(entryManager.getRelevantEntry(q), av);
			changed |= changedprime;
		} catch (NoEntryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	if (changedprime) {
    		Utilities.out("- [FINISHED] COPY TUPLES OF INPUT REGISTERS OF CALLED METHOD TO THE SUMMARYMANAGER WITH CHANGES FOR ENTRY "+ entry);
    	} else {
    		Utilities.out("- [FINISHED] COPY TUPLES OF INPUT REGISTERS OF CALLED METHOD TO THE SUMMARYMANAGER WITH NO CHANGES FOR ENTRY " + entry);	
    	}
    	
    	// UPDATE ACTUAL INFORMATION WITH OUTPUT OF ENTRY CALLED
    	AbstractValue output = null;
    	try {
			output = summaryManager.getSummaryOutput(entryManager.getRelevantEntry(q));
		} catch (NoEntryException e) {
			e.printStackTrace();
		}
    	
    	if (output != null){
    		Utilities.out("- [INIT] CHANGE REGISTERS FROM THE CALLED METHOD TO THE CALLER METHOD FOR ENTRY " +entry);
    		STuples shar = output.getSComp();
    		CTuples cycl = output.getCComp();
    		
    		List<Register> paramCalledRegisters = new ArrayList<>();
    		List<Register> paramCallerRegisters = new ArrayList<>();
    		try {
				for (int i = begin; i < entryManager.getRelevantEntry(q).getMethod().getParamWords(); i++) {
					Register r = entryManager.getRelevantEntry(q).getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, entryManager.getRelevantEntry(q).getMethod().getParamTypes()[i]);
					if (r.getType().isPrimitiveType()) continue;
					paramCalledRegisters.add(r);
				}
				
				for (int i = begin; i < q.getUsedRegisters().size(); i++) {
					RegisterOperand r = q.getUsedRegisters().get(i);
					if (r.getRegister().getType().isPrimitiveType()) continue;
					paramCallerRegisters.add(r.getRegister());
				}
			} catch (NoEntryException e) {
				e.printStackTrace();
			}
			
			if (paramCalledRegisters.isEmpty() || paramCallerRegisters.isEmpty()) {
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
				changed |= relCycle.condAdd(entry,p.val0, p.val1);
			}
			for(chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet> qu : shareMoved){
				changed |= relShare.condAdd(entry,qu.val0, qu.val1, qu.val2, qu.val3);
			}
			Utilities.out("- [FINISHED] COPY BEFORE TUPLES TO RELS OF CURRENT METHOD");
    	}
    	*/
    	
    	// DEBUG
		relShare.output();
    	relCycle.output();
		Utilities.end("PROCESSING INVOKE INSTRUCTION: " + q);
    	return changed;
    }

    private boolean isIgnorableInvoke(Quad q) {
    	return Invoke.getMethod(q).getMethod().getName().toString().equals("<init>") &&
    	Invoke.getMethod(q).getMethod().getDeclaringClass().toString().equals("java.lang.Object");
    }
    
    public void printOutput() {
    	
    	Hashtable<String, Pair<Register,Register>> registers = RegisterManager.printVarRegMap(method);
    	
		for (Pair<Register,Register> p : registers.values()) 
			for(Pair<Register,Register> q : registers.values())
				program.getAccumulatedTuples().askForS(entry, p.val0, q.val0);
		for (Pair<Register,Register> p : registers.values()) 
				program.getAccumulatedTuples().askForC(entry, p.val0);
	}
    
    /**
     * Inserts in the Quad queue all Quads which depend on {@code q} according
     * to USE-DEF analysis (i.e., Quads which are using variables defined by
     * {@code q}). Currently, all quads of a method are inserted in the queue,
     * but this is clearly sub-optimal.
     * 
     * @param q The Quad to be processed.
     */
    // protected void wakeUp(Quad q) {
    // 	queue.fill_fw(getMethod());
    //}

	public void save() {
		relShare.save();
		relCycle.save();
	}
}	
