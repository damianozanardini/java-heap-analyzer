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
		boolean b = processQuad(q);
		b |= processAfter(q);
		return b;
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
     * This method simply copies the information about the array register
     * into the destination, unless the latter has primitive type.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processALoad(Quad q) {
    	Utilities.begin("PROCESSING ALOAD INSTRUCTION: " + q);
    	if (((RegisterOperand) ALoad.getDest(q)).getType().isPrimitiveType())
    		return propagate(q);
    	AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue avIp = avI.clone();
        Register base = ((RegisterOperand) ALoad.getBase(q)).getRegister();
        Register dest = ((RegisterOperand) ALoad.getDest(q)).getRegister();
    	avIp.copyInfo(base,dest);    		
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
    	Utilities.end("PROCESSING ALOAD INSTRUCTION: " + q + " - " + b);
    	return b;
    }
    
    /**
     * This method simply copies the information about the value into the array
     * register, unless the value has primitive type. Information about the source
     * register is not kept
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processAStore(Quad q) {
    	Utilities.begin("PROCESSING ASTORE INSTRUCTION: " + q);
    	if (((RegisterOperand) AStore.getValue(q)).getType().isPrimitiveType())
    		return propagate(q);
    	AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue avIp = avI.clone();
    	avIp = avI.clone();
    	Register base = ((RegisterOperand) AStore.getBase(q)).getRegister();
    	Register value = ((RegisterOperand) AStore.getValue(q)).getRegister();
    	avIp.moveInfo(value,base);
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
		Utilities.end("PROCESSING ASTORE INSTRUCTION: " + q + " - " + b);
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
       	if (((RegisterOperand) Getfield.getDest(q)).getType().isPrimitiveType()) {
       		Utilities.info("IGNORING GETFIELD INSTRUCTION: " + q);
       		return propagate(q);
       	}
       	Utilities.begin("PROCESSING GETFIELD INSTRUCTION: " + q);
    	// I_s
    	AbstractValue avI = (GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q))).clone();
    	Utilities.info("OLD AV: " + avI);
    	Register base = ((RegisterOperand) Getfield.getBase(q)).getRegister();
    	Register dest = ((RegisterOperand) Getfield.getDest(q)).getRegister();
    	jq_Field field = ((FieldOperand) Getfield.getField(q)).getField();
    	// verifying if base.field can be non-null; if not, then no new information is
    	// produced because dest is certainly null
    	// WARNING: add this to the paper?
    	List<Pair<FieldSet,FieldSet>> selfSH = avI.getSinfo(base,base);
    	boolean shareOnField = false;
    	for (Pair<FieldSet,FieldSet> p : selfSH) {
    		shareOnField |= (p.val0 == FieldSet.addField(FieldSet.emptyset(),field) &&
    			p.val1 == p.val0);    			
    	}
    	if (!shareOnField) {
       		Utilities.info("v.f==null: NO NEW INFO PRODUCED");
       		boolean p = propagate(q);
        	Utilities.end("PROCESSING GETFIELD INSTRUCTION: " + q + " - " + p);
       		return p;
    	}
    	// I'_s
    	AbstractValue avIp = avI.clone();
    	// copy cyclicity from base to dest, as it is (not in JLAMP paper)
    	avIp.copyCinfo(base,dest);
    	// copy cyclicity of base into self-"reachability" of dest (not in JLAMP paper)
    	avIp.copyFromCycle(base,dest);
    	// copy self-sharing from self-sharing of base, removing the field
    	for (Pair<FieldSet,FieldSet> p : avI.getSinfo(base,base)) {
    		FieldSet fs0 = FieldSet.removeField(p.val0,field);
    		FieldSet fs1 = FieldSet.removeField(p.val1,field);
    		avIp.addSinfo(dest,dest,fs0,fs1);
    	}
    	int m = entry.getMethod().getCFG().getRegisterFactory().size();
    	for (int i=0; i<m; i++) {
    		Register w = entry.getMethod().getCFG().getRegisterFactory().get(i);
    		for (Pair<FieldSet,FieldSet> p : avI.getSinfo(base,w)) {
    			FieldSet fs0 = FieldSet.removeField(p.val0,field);
    			FieldSet fs1 = FieldSet.addField(p.val1,field);
    			avIp.addSinfo(dest,w,fs0,fs1);    			
    		}
    	}
    	
    	// WARNING: pay attention to these lines: is it really possible to
    	// simplify things to such an extent with respect to TOCL?
    	/*
    	// add "reachability" from the "reachability" from base, removing the field
    	for (Pair<Register,FieldSet> p : avIp.getSinfoReachingRegister(base)) {
    		FieldSet fs1 = FieldSet.removeField(p.val1,field);
    		avIp.addSinfo(dest,p.val0,fs1,FieldSet.emptyset());
    		// the old field set is still there
    		avIp.addSinfo(dest,p.val0,p.val1,FieldSet.emptyset());
    	}
    	// add "reachability" from the "reachability" to base, adding the field
    	for (Pair<Register,FieldSet> p : avIp.getSinfoReachedRegister(base)) {
    		FieldSet fs2 = FieldSet.addField(p.val1,field);
    		avIp.addSinfo(p.val0,dest,fs2,FieldSet.emptyset());
    	}
    	// add "reachability" to dest and sharing between r and dest from
    	// sharing between r and base 
    	for (Trio<Register,FieldSet,FieldSet> p : avIp.getSinfoFirstRegister(base)) {
    		if (p.val1.containsOnly(field))
    			avIp.addSinfo(p.val0,dest,p.val2,FieldSet.emptyset());
    		FieldSet fs3 = FieldSet.removeField(p.val1,field);
    		avIp.addSinfo(base,p.val0,p.val2,fs3);
    		avIp.addSinfo(base,p.val0,p.val2,p.val1);
    	}
    	for (Trio<Register,FieldSet,FieldSet> p : avIp.getSinfoSecondRegister(base)) {
    		if (p.val2.containsOnly(field))
    			avIp.addSinfo(p.val0,dest,p.val1,FieldSet.emptyset());
    		FieldSet fs4 = FieldSet.removeField(p.val2,field);
    		avIp.addSinfo(base,p.val0,p.val1,fs4);
    		avIp.addSinfo(base,p.val0,p.val1,p.val2);
    	}
    	*/
    	boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
		Utilities.info("NEW AV: " + GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q)));
    	Utilities.end("PROCESSING GETFIELD INSTRUCTION: " + q + " - " + b);
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
    	AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
      	Register r = ((RegisterOperand) New.getDest(q)).getRegister();
      	AbstractValue avIp = avI.clone();
		avIp.addSinfo(r,r,FieldSet.emptyset(),FieldSet.emptyset());
		avIp.addCinfo(r,FieldSet.emptyset());
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
		Utilities.info("OLD AV: " + GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q)));
    	Utilities.info("NEW AV: " + GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q)));
    	Utilities.end("PROCESSING NEW INSTRUCTION: " + q + " - " + b);
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
      	Register r = ((RegisterOperand) New.getDest(q)).getRegister();
      	AbstractValue av_after = av_before.clone();
		av_after.addSinfo(r,r,FieldSet.emptyset(),FieldSet.emptyset());
		av_after.addCinfo(r,FieldSet.emptyset());
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
    	Utilities.end("PROCESSING NEWARRAY INSTRUCTION: " + q + " - " + b);
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
    	if (op instanceof AConstOperand) // null
    		b = propagate(q);
    	if (op instanceof RegisterOperand) {
    		AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    		AbstractValue avIp = avI.clone();
    		Register src = ((RegisterOperand) op).getRegister();
    		Register dest = ((RegisterOperand) Move.getDest(q)).getRegister();
    		if (src.isTemp() && !dest.isTemp()) avIp.moveInfo(src,dest); 
    		else avIp.copyInfo(src,dest);    			
    		b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
    	}
		Utilities.info("OLD AV: " + GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q)));
		Utilities.info("NEW AV: " + GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q)));
    	Utilities.end("PROCESSING MOVE INSTRUCTION: " + q + " - " + b);
    	return b;
    }
    
    /**
     * Copies all tuples about each of the source registers into the
     * destination register.
     * 
     * @param q the Quad element
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
    	av1_copy.moveInfo(src1,dest);
    	AbstractValue av2 = GlobalInfo.getAV(GlobalInfo.getFinalPP(pbb2));
    	AbstractValue av2_copy = av2.clone();
    	av2_copy.moveInfo(src2,dest);
    	// both branches are joined
    	av1_copy.update(av2_copy);
    	boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av1_copy);
    	Utilities.end("PROCESSING PHI INSTRUCTION: " + q + " - " + b);
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
    	if (Putfield.getSrc(q) instanceof AConstOperand) {
    		Utilities.info("IGNORING PUTFIELD INSTRUCTION: " + q);
    		return false;
    	}
    	if (((RegisterOperand) Putfield.getSrc(q)).getType().isPrimitiveType()) {
    		Utilities.info("IGNORING PUTFIELD INSTRUCTION: " + q);
    		return false;
    	}
    	Utilities.begin("PROCESSING PUTFIELD INSTRUCTION: " + q);
    	// I_s
    	AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
		Utilities.info("OLD AV: " + avI);
		// v
    	Register v = ((RegisterOperand) Putfield.getBase(q)).getRegister();
    	// rho
    	Register rho = ((RegisterOperand) Putfield.getSrc(q)).getRegister();
    	jq_Field field = ((FieldOperand) Putfield.getField(q)).getField();
    	// I'_s
    	AbstractValue avIp = avI.clone();
    	int m = entry.getMethod().getCFG().getRegisterFactory().size();
    	// I''_s
    	AbstractValue avIpp = GlobalInfo.createNewAV(entry);

    	FieldSet z1 = FieldSet.addField(FieldSet.emptyset(),field);
    	List<Pair<FieldSet,FieldSet>> mdls_rhov = avIp.getSinfo(rho,v);
		ArrayList<FieldSet> z2 = new ArrayList<FieldSet>();
    	for (Pair<FieldSet,FieldSet> p : mdls_rhov) {
    		if (p.val1 == FieldSet.emptyset())
    			z2.add(p.val0);
    	}
    	for (int i=0; i<m; i++) {
    		for (int j=0; j<m; j++) {
    	    	Register w1 = entry.getMethod().getCFG().getRegisterFactory().get(i);
    	    	Register w2 = entry.getMethod().getCFG().getRegisterFactory().get(j);
    	    	// case (a)
    	    	for (Pair<FieldSet,FieldSet> omega1 : avIp.getSinfo(w1,v)) {
    	    		for (Pair<FieldSet,FieldSet> omega2 : avIp.getSinfo(rho,w2)) {
    	    			if (omega1.val1 == FieldSet.emptyset()) {
    	    				FieldSet fsL_a = FieldSet.union(z1,omega1.val0);
    	    				fsL_a = FieldSet.union(fsL_a,omega2.val0);
    	    				FieldSet fsR_a = omega2.val1;
    	    				avIpp.addSinfo(w1,w2,fsL_a,fsR_a);
    	    				for (FieldSet z2_fs : z2)
        	    				avIpp.addSinfo(w1,w2,FieldSet.union(fsL_a,z2_fs),fsR_a);
    	    			}
    	    		}
    	    	}
    	    	// case (b)
    	    	for (Pair<FieldSet,FieldSet> omega2 : avIp.getSinfo(v,w2)) {
        	    	for (Pair<FieldSet,FieldSet> omega1 : avIp.getSinfo(w1,rho)) {
        	    		if (omega2.val0 == FieldSet.emptyset()) {
        	    			FieldSet fsR_b = FieldSet.union(omega1.val1,z1);
        	    			fsR_b = FieldSet.union(fsR_b,omega2.val1);
    	    				FieldSet fsL_b = omega1.val0;
    	    				avIpp.addSinfo(w1,w2,fsL_b,fsR_b);
    	    				for (FieldSet z2_fs : z2)
        	    				avIpp.addSinfo(w1,w2,fsL_b,FieldSet.union(fsR_b,z2_fs));
        	    		}
        	    	}
    	    	}
    	    	// case (c)
    	    	for (Pair<FieldSet,FieldSet> omega1 : avIp.getSinfo(w1,v)) {
    	    		for (Pair<FieldSet,FieldSet> omega2 : avIp.getSinfo(v,w2)) {
    	    			if (omega1.val1 == FieldSet.emptyset() && omega2.val0 == FieldSet.emptyset()) {
    	    				for (Pair<FieldSet,FieldSet> omega : avIp.getSinfo(rho,rho)) {
    	    					FieldSet fsL_c = FieldSet.union(omega1.val0,omega.val0);
    	    					fsL_c = FieldSet.union(fsL_c,z1);
    	    					FieldSet fsR_c = FieldSet.union(omega2.val1,omega.val1);
    	    					fsR_c = FieldSet.union(fsR_c,z1);
    	    					avIpp.addSinfo(w1,w2,fsL_c,fsR_c);
        	    				for (FieldSet z2_fs1 : z2)
            	    				for (FieldSet z2_fs2 : z2)
            	    					avIpp.addSinfo(w1,w2,FieldSet.union(fsL_c,z2_fs1),FieldSet.union(fsR_c,z2_fs2));
    	    				}
    	    			}
    	    		}
    	    	}
    		}
    	}    	
    	// WARNING: this is the old code: check if it can be really removed
    	/*
    	// add "reachability" created by the new path
    	for (Pair<Register,FieldSet> p1 : avIp.getSinfoReachedRegister(base)) {
        	for (Pair<Register,FieldSet> p2 : avIp.getSinfoReachingRegister(src)) {
    			FieldSet fs1 = FieldSet.union(p1.val1,FieldSet.addField(p2.val1,field));//left
    			avIp.addSinfo(p1.val0,p2.val0,fs1,FieldSet.emptyset());//
    			avIp.addSinfo(p1.val0,p1.val0,fs1,fs1);
    			for (FieldSet fs2 : avIp.getSinfoReachingReachedRegister(src,base)) {
    				FieldSet fs3 = FieldSet.union(fs1,fs2);
    				avIp.addSinfo(p1.val0,p2.val0,fs3,FieldSet.emptyset());
    				avIp.addSinfo(p1.val0,p1.val0,fs3,fs3);
    			}
    		}
    	}
    	// add cyclicity of variables "reaching" base
    	for (Pair<Register,FieldSet> p : avIp.getSinfoReachedRegister(base)) {
    		for (FieldSet fs : avIp.getSinfoReachingReachedRegister(src,base)) {
    			FieldSet fs0 = FieldSet.addField(fs,field);
    			avIp.addCinfo(p.val0,fs0);
    			avIp.addSinfo(p.val0,p.val0,fs0,fs0);
    		}
    	}
    	// copy cyclicity of src into variables which "reach" base
    	for (Pair<Register,FieldSet> p : avIp.getSinfoReachedRegister(base)) {
    		avIp.copyCinfo(src,p.val0);
    		avIp.copyFromCycle(src,p.val0);
    	}
        // add sharing from sharing
    	for (Trio<Register,FieldSet,FieldSet> t : avIp.getSinfoFirstRegister(src)) {
    		FieldSet fs = FieldSet.addField(t.val1,field);
    		avIp.addSinfo(base,t.val0,fs,t.val2);
    	}
    	for (Trio<Register,FieldSet,FieldSet> t : avIp.getSinfoSecondRegister(src)) {
    		FieldSet fs = FieldSet.addField(t.val2,field);
    		avIp.addSinfo(t.val0,base,t.val1,fs);
    	}
    	*/
    	// WARNING: cyclicity currently missing
    	boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
    	b |= GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIpp);
		Utilities.info("NEW AV: " + GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q)));
    	Utilities.end("PROCESSING PUTFIELD INSTRUCTION: " + q + " - " + b);
    	return b;
    }
    
    /**
     * This method takes an invoke Quad q and processes it. This roughly corresponds to Figure 7
     * in the first paper submitted to JLAMP, and includes: 
     * - Update the input of the called entry with the tuples of the registers which are
     *   passed as parameters.
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

    	// WARNING: purity analysis is not supported
    	try {
    		// the entry invoked from the present quad. WARNING: due to inheritance, there could
    		// be more than one entry here, and this SHOULD be taken into account
    		Entry invokedEntry = GlobalInfo.getEntryManager().getRelevantEntry(q);

    		// collecting actual parameters as a list of registers
    		ArrayList<Register> actualParameters = new ArrayList<Register>();
    		for (int i=0; i<Invoke.getParamList(q).length(); i++)
    			actualParameters.add(Invoke.getParamList(q).get(i).getRegister());
    		
    		// I_s in the paper
    		AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
        	Utilities.info("I_s = " + avI);

    		// copy of I_s
        	AbstractValue avIp = avI.clone();
        	// only actual parameters are kept; av_Ip becomes I'_s in the paper
        	avIp.filterActual(actualParameters);
        	Utilities.info("I'_s = " + avIp);
        	
        	// this produces I'_s[\bar{v}/mth^i] in the paper, where the abstract information
        	// is limited to the formal parameters of the invoked entry
        	avIp.actualToFormal(actualParameters,invokedEntry.getMethod());
        	// I'_s[\bar{v}/mth^i] is used to update the input of the summary of the invoked entry;
        	// if the new information is not included in the old one, then the invoked entry needs
        	// to be re-analyzed
        	//
        	// WARNING: this boolean variable is not used properly because Heap.run() is not optimized:
        	// at every program-level iteration all the code is reanalyzed. Anyway, it currently can
        	// still trigger the next iteration. The optimization would be to only wake up selected
        	// entries (this can be done by using a queue of entries instead of a loop on all Entry
        	// objects, which, by the way, avoids analyzing methods which are never executed) 
        	boolean needToWakeUp = GlobalInfo.summaryManager.updateSummaryInput(invokedEntry,avIp);
        	if (needToWakeUp) GlobalInfo.wakeUp(invokedEntry);
        	b |= needToWakeUp;
        	// this is \absinterp(mth)(I'_s[\bar{v}/mth^i]), although, of course, the input and output
        	// components of a summary are not "synchronized" (we just produced a "new" input, but we
        	// are still using the "old" output while waiting the entry to be re-analyzed)
        	AbstractValue avIpp = GlobalInfo.summaryManager.getSummaryOutput(invokedEntry);
        	// this generates I''_s, which could be empty if no summary output is available
        	//
        	// WARNING: have to take the return value into account
        	if (avIpp != null) {
        		avIpp.cleanGhostRegisters(invokedEntry);
        		avIpp.formalToActual(actualParameters,invokedEntry.getMethod());
        	} else avIpp = GlobalInfo.createNewAV(entry);
        	Utilities.info("I''_s = " + avIpp);

        	// start computing I'''_s
        	Utilities.begin("COMPUTING I'''_s");
        	AbstractValue avIppp = GlobalInfo.createNewAV(entry);
        	int m = entry.getMethod().getCFG().getRegisterFactory().size();
        	int n = actualParameters.size();
        	AbstractValue[][] avs = new AbstractValue[n][n];
        	// computing each I^{ij}_s
        	for (int i=0; i<n; i++) {
        		for (int j=0; j<n; j++) {
        			avs[i][j] = GlobalInfo.createNewAV(entry);
        			// WARNING: can possibly filter out non-reference registers
        			Register vi = actualParameters.get(i);
        			Register vj = actualParameters.get(j);
        			for (int k1=0; k1<m; k1++) { // for each w_1 
            			for (int k2=0; k2<m; k2++) { // for each w_2
            				Register w1 = entry.getMethod().getCFG().getRegisterFactory().get(k1);
            				Register w2 = entry.getMethod().getCFG().getRegisterFactory().get(k2);
            				for (Pair<FieldSet,FieldSet> pair_1 : avI.getSinfo(w1,vi)) { // \omega_1(toRight)
                				for (Pair<FieldSet,FieldSet> pair_2 : avI.getSinfo(vj,w2)) { // \omega_2(toLeft)
                    				for (Pair<FieldSet,FieldSet> pair_ij : avIpp.getSinfo(vi,vj)) { // \omega_ij
                    					Utilities.info("FOUND: vi = " + vi + ", vj = " + vj + ", w1 = " + w1 + ", w2 = " + w2 + ", pair_1 = " + pair_1 + ", pair_2 = " + pair_2 + ", pair_ij = " + pair_ij);
                    					for (Pair<FieldSet,FieldSet> newPairs : getNewPairs(pair_1,pair_2,pair_ij))
                    						avs[i][j].addSinfo(w1,w2,newPairs.val0,newPairs.val1);
                    				}                    					
                				}
            				}
            			}
        			}
        		}
        	}
        	// joining all together into I'''_s
        	for (int i=0; i<n; i++) {
        		for (int j=0; j<n; j++) {
        			avIppp.update(avs[i][j]);
        		}
        	}
        	Utilities.end("COMPUTING I'''_s = " + avIppp);
        	
        	// computing the final union I_s \vee I'_s \vee I''_s \vee I'''_s
        	// WARNING I''''_s is still not here
        	AbstractValue avOut = avI.clone();
        	avOut.removeInfoList(actualParameters);
        	avOut.update(avIpp);
        	avOut.update(avIppp);
        	Utilities.info("FINAL UNION: " + avOut);
        	
        	b |= GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avOut);
    	} catch (NoEntryException nee) { // this should never happen
			nee.printStackTrace();
			return false;
    	}
    	Utilities.info("FINAL AV: " + GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q)));
		Utilities.end("PROCESSING INVOKE INSTRUCTION: " + q + " - " + b);
    	return b;
    }

    private ArrayList<Pair<FieldSet,FieldSet>> getNewPairs(Pair<FieldSet, FieldSet> pair_1,
			Pair<FieldSet, FieldSet> pair_2, Pair<FieldSet, FieldSet> pair_ij) {
    	// initially empty
    	ArrayList<Pair<FieldSet,FieldSet>> pairs = new ArrayList<Pair<FieldSet,FieldSet>>();
    	
    	FieldSet fs_l = pair_1.val0;
    	FieldSet fs_r = pair_2.val1;
    	for (FieldSet omega_i : FieldSet.inBetween(pair_1.val1,pair_ij.val0)) {
    		for (FieldSet omega_j : FieldSet.inBetween(pair_2.val0,pair_ij.val1)) {
    			fs_l = FieldSet.union(fs_l,omega_i);
    			fs_r = FieldSet.union(fs_r,omega_j);
    			pairs.add(new Pair<FieldSet,FieldSet>(fs_l,fs_r));
    		}
    	}
		return pairs;
	}    
    
	/**
     * Returns true iff the method invoked by the Quad q is <init>:()V@java.lang.Object
     * 
     * @param q the Quad element (it is an invoke Quad)
     * @return whether q is invoking <init>:()V@java.lang.Object
     */
    protected boolean isIgnorableInvoke(Quad q) {
    	return Invoke.getMethod(q).getMethod().getName().toString().equals("<init>") &&
    	Invoke.getMethod(q).getMethod().getDeclaringClass().toString().equals("java.lang.Object");
    }
        
    /**
     * Takes the abstract value before a quad and add it to the current abstract
     * value after the same quad.
     *  
     * @param q the Quad element
     * @return whether the abstract information after q has changed
     */
    protected boolean propagate(Quad q) {
    	AbstractValue av_before = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    	AbstractValue av_after;
    	if (av_before == null) { // no info at the program point before q
    		av_after = GlobalInfo.createNewAV(entry);
    	} else {
    		av_after = av_before.clone();
    	}
		return GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av_after);
    }
    
    /**
     * Copies the abstract value at the end of a block into the beginning of each
     * successor block.  If a successor block has no Quads, then, in turns, its
     * successors are also considered (this is done by using the queue).
     * 
     * @param q the Quad element
     * @return whether the abstract information has changed somewhere (in some 
     * of the program points affected)
     */
    protected boolean processAfter(Quad q) {
    	boolean b = false;
    	BasicBlock bb = q.getBasicBlock();
    	// WARNING: take possible register renaming into account
    	if (bb.getLastQuad() == q) { // last Quad of the current basic block
    		List<BasicBlock> bbs = bb.getSuccessors();
    		Utilities.info("PROPAGATING AV TO SUCC BLOCKS: " + bbs);
    		LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>(bbs); 
    		AbstractValue av = GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q));
    		while (!queue.isEmpty()) {
    			BasicBlock succ = queue.removeFirst();
    			ProgramPoint pp = GlobalInfo.getInitialPP(succ);
    			AbstractValue av_copy = av.clone();
    			b |= GlobalInfo.update(pp,av_copy);
    			if (succ.getQuads().size() == 0) queue.addAll(succ.getSuccessors());    			
    		}
    	}
    	return b;
    }
        
}	
