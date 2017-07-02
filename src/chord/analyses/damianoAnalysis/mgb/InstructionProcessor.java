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
    	AbstractValue avIp = avI.propagateGetfield(entry,q,base,dest,field);
    	
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
    	AbstractValue avIp = avI.propagatePutfield(entry,q,v,rho,field);
    	boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
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
        	AbstractValue avOut = avI.propagateInvoke(entry,invokedEntry,q,actualParameters);
        	b |= GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avOut);
    	} catch (NoEntryException nee) { // this should never happen
			nee.printStackTrace();
			return false;
    	}
    	Utilities.info("FINAL AV: " + GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q)));
		Utilities.end("PROCESSING INVOKE INSTRUCTION: " + q + " - " + b);
    	return b;
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
    		Utilities.begin("PROPAGATING AV TO SUCC BLOCKS: " + bbs);
    		LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>(bbs); 
    		AbstractValue av = GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q));
    		Utilities.info("AV: " + av);
    		while (!queue.isEmpty()) {
    			BasicBlock succ = queue.removeFirst();
        		Utilities.info("BB: " + succ);
    			ProgramPoint pp = GlobalInfo.getInitialPP(succ);
        		Utilities.info("PP: " + pp);
    			AbstractValue av_copy = av.clone();
        		Utilities.info("AV_COPY: " + av_copy);
    			b |= GlobalInfo.update(pp,av_copy);
    			if (succ.getQuads().size() == 0) queue.addAll(succ.getSuccessors());    			
    		}
    		Utilities.end("PROPAGATING AV TO SUCC BLOCKS: " + bbs);
    	}
    	return b;
    }
        
}	
