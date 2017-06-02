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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Fixpoint;
import chord.analyses.damianoAnalysis.ProgramPoint;
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
	protected Entry entry;
	protected jq_Method method;
	
	public InstructionProcessor(Entry e) {
		entry = e;
		method = entry.getMethod();
	}
    
	/**
	 * This method does two things:
	 * - given abstract information before q, it computes the abstrac
	 *   information after q (calling process(q))
	 * - if q is the last instruction in a block, the computed abstract
	 *   value is propagated to the beginning of each successor block
	 * @param q
	 * @return
	 */
	public boolean process(Quad q) {
		return processQuad(q) | processAfter(q);	
	}
	
    /**
     * This method processes a Quad object {@code q}, branching on the operator.
     * 
     * @param q The Quad to be processed.
     * @return whether new tuples have been added.
     */
	// WARNING: make sure that all cases are covered now that this class no longer
	// inherits from Fixpoint
    protected boolean processQuad(Quad q) {
    	
    	Operator operator = q.getOperator();
    	if (operator instanceof ALength) {
    		Utilities.info("IGNORING ALENGTH INSTRUCTION: " + q);
    		propagate(q);
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
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof BoundsCheck) {
    		Utilities.info("IGNORING BOUNDSCHECK INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Branch) {
    		Utilities.info("IGNORING BRANCH INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof CheckCast) {
    		Utilities.info("IGNORING CHECKCAST INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Getfield) {
    		return processGetfield(q);
    	}
    	if (operator instanceof Getstatic) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING GETSTATIC INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Goto) {
    		Utilities.info("IGNORING GOTO INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof InstanceOf) {
    		Utilities.info("IGNORING INSTANCEOF INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof IntIfCmp) {
    		Utilities.info("IGNORING INTIFCMP INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Invoke) {
    		// calls to <init> of the Object class can be ignored
    		if (isIgnorableInvoke(q)) {
    			Utilities.info("IGNORING INVOKE INSTRUCTION: " + q);
        		propagate(q);
    			return false;
    		} else {
    			return processInvokeMethod(q);
    		}
    	}
    	if (operator instanceof Jsr) {
    		Utilities.info("IGNORING JSR INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof LookupSwitch) {
    		// TO-DO: maybe the treatment of this instruction is needed
    		Utilities.info("IGNORING LOOKUPSWITCH INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof MemLoad) {
    		// TO-DO: not clear; currently unsupported
    		Utilities.info("IGNORING MEMLOAD INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof MemStore) {
    		// TO-DO: not clear; currently unsupported
    		Utilities.info("IGNORING MEMSTORE INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Monitor) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING MONITOR INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Move) {
    		if (operator instanceof MOVE_A)
    			return processMove(q);
    		else Utilities.info("IGNORING NON-REFERENCE MOVE INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof MultiNewArray) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING MULTINEWARRAY INSTRUCTION: " + q);
    		propagate(q);
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
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Phi) {
    		return processPhi(q);
    	}
    	if (operator instanceof Putfield) {
    		// TO-DO: check if there are other subclasses to be processed  
    		if (operator instanceof PUTFIELD_A)
    			return processPutfield(q);
    		else {
    			Utilities.info("IGNORING NON-REFERENCE PUTFIELD INSTRUCTION: " + q);
        		propagate(q);
        		return false;
    		}
    	}
    	if (operator instanceof Putstatic) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING PUTSTATIC INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Ret) {
    		Utilities.info("IGNORING RET INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Return) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING RETURN INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Special) {
    		// TO-DO: currently unsupported, not clear when it is used
    		Utilities.info("IGNORING SPECIAL INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof StoreCheck) {
    		Utilities.info("IGNORING STORECHECK INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof TableSwitch) {
    		// TO-DO: currently unsupported
    		Utilities.info("IGNORING TABLESWITCH INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof Unary) {
    		// TO-DO: subclasses involving addresses and object
    		// (ADDRESS_2OBJECT, OBJECT_2ADDRESS) unsupported
    		Utilities.info("IGNORING UNARY INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	if (operator instanceof ZeroCheck) {
    		Utilities.info("IGNORING ZEROCHECK INSTRUCTION: " + q);
    		propagate(q);
    		return false;
    	}
    	// This should never happen
    	Utilities.warn("CANNOT DEAL WITH QUAD" + q);
		propagate(q);
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
    	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue av_after;
    	if (av_before == null) { // no info at the program point before q
    		av_after = new AbstractValue();
    	} else {
    		av_after = av_before.clone();
        	Register base = ((RegisterOperand) ALoad.getBase(q)).getRegister();
        	Register dest = ((RegisterOperand) ALoad.getDest(q)).getRegister();
    		av_after.copyTuples(base,dest);    		
    	}
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
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
    	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue av_after;
    	if (av_before == null) { // no info at the program point before q
    		av_after = new AbstractValue();
    	} else {
    		av_after = av_before.clone();
        	Register base = ((RegisterOperand) AStore.getBase(q)).getRegister();
        	Register value = ((RegisterOperand) AStore.getValue(q)).getRegister();
    		av_after.moveTuples(value,base);
    	}
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
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
    	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue av_after = av_before.clone();
    	STuples sh_after = av_after.getSComp();
    	CTuples cy_after = av_after.getCComp();
    	// copy cyclicity from base to dest
    	cy_after.copyTuples(base,dest);
    	// copy self-"reachability" of dest from from cyclicity of base
    	sh_after.copyTuplesFromCycle(base,dest,av_after.getCComp());
    	// add "reachability" from the "reachability" from base, removing the field
    	for (Pair<Register,FieldSet> p : sh_after.findTuplesByReachingRegister(base)) {
    		FieldSet fs1 = FieldSet.removeField(p.val1,field);
    		sh_after.addTuple(dest,p.val0,fs1,FieldSet.emptyset());
    		// the old field set is still there
    		sh_after.addTuple(dest,p.val0,p.val1,FieldSet.emptyset());
    	}
    	// add "reachability" from the "reachability" to base, adding the field
    	for (Pair<Register,FieldSet> p : sh_after.findTuplesByReachedRegister(base)) {
    		FieldSet fs2 = FieldSet.addField(p.val1,field);
    		sh_after.addTuple(p.val0,dest,fs2,FieldSet.emptyset());
    	}
    	// add "reachability" to dest and sharing between r and dest from
    	// sharing between r and base 
    	for (Trio<Register,FieldSet,FieldSet> p : sh_after.findTuplesByFirstRegister(base)) {
    		if (p.val1.containsOnly(field))
    			sh_after.addTuple(p.val0,dest,p.val2,FieldSet.emptyset());
    		FieldSet fs3 = FieldSet.removeField(p.val1,field);
    		sh_after.addTuple(base,p.val0,p.val2,fs3);
    		sh_after.addTuple(base,p.val0,p.val2,p.val1);
    	}
    	for (Trio<Register,FieldSet,FieldSet> p : sh_after.findTuplesBySecondRegister(base)) {
    		if (p.val2.containsOnly(field))
    			sh_after.addTuple(p.val0,dest,p.val1,FieldSet.emptyset());
    		FieldSet fs4 = FieldSet.removeField(p.val2,field);
    		sh_after.addTuple(base,p.val0,p.val1,fs4);
    		sh_after.addTuple(base,p.val0,p.val1,p.val2);
    	}
    	boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
    	Utilities.end("PROCESSING GETFIELD INSTRUCTION: " + q);
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
    protected boolean processNew(Quad q) {
    	Utilities.begin("PROCESSING NEW INSTRUCTION: " + q);
    	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue av_after;
      	Register r = ((RegisterOperand) New.getDest(q)).getRegister();
    	if (av_before == null) { // no info at the program point before q
    		av_after = new AbstractValue();
    	} else {
    		av_after = av_before.clone();
    	}
		av_after.getSComp().addTuple(r,r,FieldSet.emptyset(),FieldSet.emptyset());
		av_after.getCComp().addTuple(r,FieldSet.emptyset());
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
		Utilities.info("OLD AV: " + GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q)));
    	Utilities.info("NEW AV: " + GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q)));
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
    	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue av_after;
      	Register r = ((RegisterOperand) New.getDest(q)).getRegister();
    	if (av_before == null) { // no info at the program point before q
    		av_after = new AbstractValue();
    	} else {
    		av_after = av_before.clone();
    	}
		av_after.getSComp().addTuple(r,r,FieldSet.emptyset(),FieldSet.emptyset());
		av_after.getCComp().addTuple(r,FieldSet.emptyset());
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
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
    	Operand op = Move.getSrc(q);
    	boolean b = false;
    	if (op instanceof AConstOperand) { //null
    		propagate(q);
    		b = false;
    	}
    	if (op instanceof RegisterOperand) {
    		AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    		AbstractValue av_after;
    		if (av_before == null) { // no info at the program point before q
    			av_after = new AbstractValue();
    		} else {
    			av_after = av_before.clone();
    			Register src = ((RegisterOperand) op).getRegister();
    			Register dest = ((RegisterOperand) Move.getDest(q)).getRegister();
    			if (src.isTemp() && !dest.isTemp()) av_after.moveTuples(src,dest); 
    			else av_after.copyTuples(src,dest);    			
    		}
    		b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
    	}
		Utilities.info("OLD AV: " + GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q)));
		Utilities.info("NEW AV: " + GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q)));
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
    	Register dest = ((RegisterOperand) Phi.getDest(q)).getRegister();
    	// this list must have two elements
    	List<BasicBlock> pbbs = q.getBasicBlock().getPredecessors();
    	BasicBlock pbb1 = pbbs.get(0);
    	BasicBlock pbb2 = pbbs.get(1);
    	AbstractValue av1 = GlobalInfo.getAV(GlobalInfo.getFinalPP(pbb1));
    	AbstractValue av1_copy = av1.clone();
    	av1_copy.moveTuples(src1,dest);
    	AbstractValue av2 = GlobalInfo.getAV(GlobalInfo.getFinalPP(pbb2));
    	AbstractValue av2_copy = av2.clone();
    	av2_copy.moveTuples(src2,dest);
    	av1_copy.update(av2_copy);
    	boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av1_copy);
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
    	Register base = ((RegisterOperand) Putfield.getBase(q)).getRegister();
    	Register src = ((RegisterOperand) Putfield.getSrc(q)).getRegister();
    	jq_Field field = ((FieldOperand) Putfield.getField(q)).getField();
    	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue av_after = av_before.clone();
    	STuples sh_after = av_after.getSComp();
    	CTuples cy_after = av_after.getCComp();
    	// add "reachability" created by the new path
    	for (Pair<Register,FieldSet> p1 : sh_after.findTuplesByReachedRegister(base)) {
        	for (Pair<Register,FieldSet> p2 : sh_after.findTuplesByReachingRegister(src)) {
    			FieldSet fs1 = FieldSet.union(p1.val1,FieldSet.addField(p2.val1,field));//left
    			sh_after.addTuple(p1.val0,p2.val0,fs1,FieldSet.emptyset());//
    			sh_after.addTuple(p1.val0,p1.val0,fs1,fs1);
    			for (FieldSet fs2 : sh_after.findTuplesByReachingReachedRegister(src,base)) {
    				FieldSet fs3 = FieldSet.union(fs1,fs2);
    				sh_after.addTuple(p1.val0,p2.val0,fs3,FieldSet.emptyset());
    				sh_after.addTuple(p1.val0,p1.val0,fs3,fs3);
    			}
    		}
    	}
    	// add cyclicity of variables "reaching" base
    	for (Pair<Register,FieldSet> p : sh_after.findTuplesByReachedRegister(base)) {
    		for (FieldSet fs : sh_after.findTuplesByReachingReachedRegister(src,base)) {
    			FieldSet fs0 = FieldSet.addField(fs,field);
    			cy_after.addTuple(p.val0,fs0);
    			sh_after.addTuple(p.val0,p.val0,fs0,fs0);
    		}
    	}
    	// copy cyclicity of src into variables which "reach" base
    	for (Pair<Register,FieldSet> p : sh_after.findTuplesByReachedRegister(base)) {
    		cy_after.copyTuples(src,p.val0);
    		sh_after.copyTuplesFromCycle(src,p.val0,cy_after);
    	}
        // add sharing from sharing
    	for (Trio<Register,FieldSet,FieldSet> t : sh_after.findTuplesByFirstRegister(src)) {
    		FieldSet fs = FieldSet.addField(t.val1,field);
    		sh_after.addTuple(base,t.val0,fs,t.val2);
    	}
    	for (Trio<Register,FieldSet,FieldSet> t : sh_after.findTuplesBySecondRegister(src)) {
    		FieldSet fs = FieldSet.addField(t.val2,field);
    		sh_after.addTuple(t.val0,base,t.val1,fs);
    	}
    	boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
    	Utilities.end("PROCESSING PUTFIELD INSTRUCTION: " + q);
    	return b;
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
    	
    	boolean b = false;

    	try {
    		Entry invokedEntry = GlobalInfo.entryManager.getRelevantEntry(q);
        	ParamListOperand actualParameters = Invoke.getParamList(q);
        	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));        	
        	AbstractValue av_callInput = av_before.clone();
        	av_callInput.actualToFormal(actualParameters,invokedEntry.getMethod());
        	
        	// WARNING: this boolean variable is not used properly because Heap.run() is not optimized:
        	// at every program-level iteration all the code is reanalyzed. Anyway, it can still trigger
        	// the next iteration
        	boolean needToWakeUp = GlobalInfo.summaryManager.updateSummaryInput(invokedEntry,av_callInput);
        	b |= needToWakeUp;
        	
        	AbstractValue av_callOutput = GlobalInfo.summaryManager.getSummaryOutput(invokedEntry);
        	if (av_callOutput != null)
        		av_callOutput.formalToActual(actualParameters,invokedEntry.getMethod());
        	AbstractValue av_beforeFiltered = av_before.clone();
        	for (int i = 0; i<actualParameters.length(); i++) {
        		av_beforeFiltered.remove(actualParameters.get(i).getRegister());
        		Utilities.info("REMOVED " + actualParameters.get(i).getRegister());
        	}
        	if (av_callOutput != null)
        		av_callOutput.update(av_beforeFiltered);
        	else av_callOutput = av_beforeFiltered;
        	b |= GlobalInfo.update(GlobalInfo.getPPBefore(entry,q),av_callOutput);
    	} catch (NoEntryException nee) { // this should never happen
			nee.printStackTrace();
			return false;
    	}
		Utilities.end("PROCESSING INVOKE INSTRUCTION: " + q);
    	return b;
    }

    protected boolean isIgnorableInvoke(Quad q) {
    	return Invoke.getMethod(q).getMethod().getName().toString().equals("<init>") &&
    	Invoke.getMethod(q).getMethod().getDeclaringClass().toString().equals("java.lang.Object");
    }
        
    // we discard the final boolean value because it is never the case that
    // simply propagating an abstract value is what triggers the next iteration
    protected void propagate(Quad q) {
    	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue av_after;
    	if (av_before == null) { // no info at the program point before q
    		av_after = new AbstractValue();
    	} else {
    		av_after = av_before.clone();
    	}
		GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
    }
    
    /**
     * This method copies the abstract value at the end of a block into the 
     * beginning of successor blocks.
     * If a successor block has no Quads, then, in turns, its successors should
     * be also considered (this is done by using the queue).
     * 
     * @param q
     * @return
     */
    protected boolean processAfter(Quad q) {
    	boolean b = false;
    	BasicBlock bb = q.getBasicBlock();
    	// WARNING: take possible register renaming into account
    	if (bb.getLastQuad() == q) { // last Quad of the current basic block
    		// WARNING: if a successor block has no Quads, the successor of
    		// the successor should be also considered
    		List<BasicBlock> bbs = bb.getSuccessors();
    		LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>(bbs); 
    		AbstractValue av = GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q));
    		while (!queue.isEmpty()) {
    			BasicBlock succ = queue.removeFirst();
    			ProgramPoint pp = GlobalInfo.getInitialPP(succ);
    			b |= GlobalInfo.update(pp,av);
    			if (succ.getQuads().size() == 0)
    				queue.addAll(succ.getSuccessors());    			
    		}
    	}
    	return b;
    }
    
}	
