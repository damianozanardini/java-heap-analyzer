package chord.analyses.damianoAnalysis.sharingCyclicity;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference.jq_NullType;
import joeq.Compiler.BytecodeAnalysis.BytecodeVisitor;
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
import joeq.Compiler.Quad.Operator.IntIfCmp.IFCMP_A;
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
import joeq.Compiler.Quad.RegisterFactory.Register;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.NoEntryException;
import chord.analyses.damianoAnalysis.ProgramPoint;
import chord.analyses.damianoAnalysis.Utilities;

/**
 * This class is in charge of applying transfer functions to each instruction
 * (Quad object).  It is designed to be implementation-agnostic: most of the
 * work is left to the AbstractValue object, whose actual type, a subclass of
 * AbstractValue, does depend on the active implementation.
 * Applying a transfer function means, in general, taking the abstract
 * information before a Quad object q, computing the new information after q,
 * and updating the global state accordingly.
 * 
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
public class TransferFunctionManager {
	
	protected Entry entry;
	protected jq_Method method;
	
	public TransferFunctionManager(Entry e) {
		entry = e;
		method = entry.getMethod();
	}
    
	/**
	 * This method does two things:
	 * - given abstract information before q, it computes the abstract
	 *   information after q (calling transferQuad(q))
	 * - if q is the last instruction in a block, the computed abstract
	 *   value is transferred to the beginning of each successor block
	 *   
	 * @param q
	 * @return
	 */
	public boolean transfer(Quad q) {
		boolean b = transferQuad(q);
		b |= transferEndBlock(q);
		return b;
	}
	
    /**
     * This method processes a Quad object {@code q}, branching on the operator.
     * 
     * @param q The Quad to be processed.
     * @return whether new tuples have been added.
     */
	protected boolean transferQuad(Quad q) {
		Operator operator = q.getOperator();
		if (operator instanceof ALength) {
			Utilities.info("IGNORING ALENGTH INSTRUCTION: " + q);
			transferSkip(q);
			return false;
		}
		if (operator instanceof ALoad) {
			return transferALoad(q);
		}
		if (operator instanceof AStore) {
			return transferAStore(q);
		}
		if (operator instanceof Binary) {
			// NOTE: it is not clear what the subclass ALIGN_P of Binary does; here
			// we assume that all subclasses manipulate primitive types  
			Utilities.info("IGNORING BINARY INSTRUCTION: " + q);
			transferSkip(q);
			return false;
		}
    		if (operator instanceof BoundsCheck) {
    			Utilities.info("IGNORING BOUNDSCHECK INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Branch) {
    			return transferBranch(q);
    		}
    		if (operator instanceof CheckCast) {
    			Utilities.info("IGNORING CHECKCAST INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Getfield) {
    			return transferGetfield(q);
    		}
    		if (operator instanceof Getstatic) {
    			// TO-DO: currently unsupported
    			Utilities.info("IGNORING GETSTATIC INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Goto) {
    			Utilities.info("IGNORING GOTO INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof InstanceOf) {
    			Utilities.info("IGNORING INSTANCEOF INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof IntIfCmp) {
    			Utilities.info("IGNORING INTIFCMP INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Invoke) {
    			// calls to <init> of the Object class can be ignored
    			if (isIgnorableInvoke(q)) {
    				Utilities.info("IGNORING INVOKE INSTRUCTION: " + q);
    				transferSkip(q);
    				return false;
    			} else {
    				return transferInvokeMethod(q);
    			}
    		}
    		if (operator instanceof Jsr) {
    			Utilities.info("IGNORING JSR INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof LookupSwitch) {
    			// TO-DO: maybe the treatment of this instruction is needed
    			Utilities.info("IGNORING LOOKUPSWITCH INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof MemLoad) {
    			// TO-DO: not clear; currently unsupported
    			Utilities.info("IGNORING MEMLOAD INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof MemStore) {
    			// TO-DO: not clear; currently unsupported
    			Utilities.info("IGNORING MEMSTORE INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Monitor) {
    			// TO-DO: currently unsupported
    			Utilities.info("IGNORING MONITOR INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Move) {
    			if (operator instanceof MOVE_A)
    				return transferMove(q);
    			else Utilities.info("IGNORING NON-REFERENCE MOVE INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof MultiNewArray) {
    			// TO-DO: currently unsupported
    			Utilities.info("IGNORING MULTINEWARRAY INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof New) {
    			return transferNew(q);
    		}
    		if (operator instanceof NewArray) {
    			return transferNewArray(q);
    		}
    		if (operator instanceof NullCheck) {
    			// TO-DO: maybe there could be some optimization here (flow-sensitive)
    			Utilities.info("IGNORING NULLCHECK INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Phi) {
    			return transferPhi(q);
    		}
    		if (operator instanceof Putfield) {
    			// TO-DO: check if there are other subclasses to be processed  
    			if (operator instanceof PUTFIELD_A)
    				return transferPutfield(q);
    			else {
    				Utilities.info("IGNORING NON-REFERENCE PUTFIELD INSTRUCTION: " + q);
    				transferSkip(q);
    				return false;
    			}
    		}
    		if (operator instanceof Putstatic) {
    			// TO-DO: currently unsupported
    			Utilities.info("IGNORING PUTSTATIC INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Ret) {
    			Utilities.info("IGNORING RET INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Return) {
    			return transferReturn(q);
    		}
    		if (operator instanceof Special) {
    			// TO-DO: currently unsupported, not clear when it is used
    			Utilities.info("IGNORING SPECIAL INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof StoreCheck) {
    			Utilities.info("IGNORING STORECHECK INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof TableSwitch) {
    			// TO-DO: currently unsupported
    			Utilities.info("IGNORING TABLESWITCH INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof Unary) {
    			// TO-DO: subclasses involving addresses and object
    			// (ADDRESS_2OBJECT, OBJECT_2ADDRESS) unsupported
    			Utilities.info("IGNORING UNARY INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		if (operator instanceof ZeroCheck) {
    			Utilities.info("IGNORING ZEROCHECK INSTRUCTION: " + q);
    			transferSkip(q);
    			return false;
    		}
    		// This should never happen
    		Utilities.warn("CANNOT DEAL WITH QUAD" + q);
    		transferSkip(q);
    		return false;
    	}

    /**
     * Copies the information about the source register into the destination, unless
     * the latter has primitive type.
     * 
     * @param q The Quad to be processed.
     */
	protected boolean transferALoad(Quad q) {
		Utilities.begin("PROCESSING ALOAD INSTRUCTION: " + q);
		if (((RegisterOperand) ALoad.getDest(q)).getType().isPrimitiveType())
			return transferSkip(q);
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
     * Copies the information about the value into the destination register, unless
     * the value has primitive type. Information about the source register is not kept.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean transferAStore(Quad q) {
    		Utilities.begin("PROCESSING ASTORE INSTRUCTION: " + q);
    		if (((RegisterOperand) AStore.getValue(q)).getType().isPrimitiveType())
    			return transferSkip(q);
    		AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    		AbstractValue avIp = avI.clone();
    		Register base = ((RegisterOperand) AStore.getBase(q)).getRegister();
    		Register value = ((RegisterOperand) AStore.getValue(q)).getRegister();
    		avIp.moveInfo(value,base);
    		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
    		Utilities.end("PROCESSING ASTORE INSTRUCTION: " + q + " - " + b);
    		return b;
    }
    
    // WARNING: to be checked (some cases have not been tested)
    protected boolean transferBranch(Quad q) {
		Utilities.begin("PROCESSING BRANCH INSTRUCTION: " + q);
    		AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    		Utilities.info("OLD AV: " + avI);
		AbstractValue avIp = avI.clone();
		if (q.getOperator() instanceof IntIfCmp) {
			IntIfCmp iic = (IntIfCmp) q.getOperator();
			if (iic instanceof IFCMP_A) {
				Register r1 = ((RegisterOperand) IFCMP_A.getSrc1(q)).getRegister();
				if (r1!=null && r1.isTemp()) avIp.removeInfo(r1);
				Operand src2 = IFCMP_A.getSrc2(q);
				if (!(src2 instanceof AConstOperand && 
					((AConstOperand) src2).getType() instanceof jq_NullType)) {
					Register r2 = ((RegisterOperand) src2).getRegister();
					if (r2!=null && r2.isTemp()) avIp.removeInfo(r2);
					Utilities.info("XXXXXXX " + r1 + " " + r2);
				}				
			}
		}
		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
		showNewAV(entry,q);
		Utilities.end("PROCESSING BRANCH INSTRUCTION: " + q + " - " + b);
		return b;
    }
    
    /**
     * Transfer function for getfield.  Most of the work is done inside the 
     * AbstractValue (actually, one of its subclasses) object.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean transferGetfield(Quad q) {
    		if (((RegisterOperand) Getfield.getDest(q)).getType().isPrimitiveType()) {
    			Utilities.info("IGNORING GETFIELD INSTRUCTION: " + q);
    			return transferSkip(q);
    		}
    		Utilities.begin("PROCESSING GETFIELD INSTRUCTION: " + q);
    		// I_s
    		AbstractValue avI = (GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q))).clone();
    		Utilities.info("OLD AV: " + avI);
    		Register base = ((RegisterOperand) Getfield.getBase(q)).getRegister();
    		Register dest = ((RegisterOperand) Getfield.getDest(q)).getRegister();
    		jq_Field field = ((FieldOperand) Getfield.getField(q)).getField();
    		AbstractValue avIp = avI.doGetfield(entry,q,base,dest,field);
    	
    		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
    		showNewAV(entry,q);
    		Utilities.end("PROCESSING GETFIELD INSTRUCTION: " + q + " - " + b);
    		return b;
    }

    /**
     * Adds sharing information (r,r,{},{}) and cyclicity information
     * (r,{}), where r contains the object created by {@code q}.  In the case
     * of sharing, the empty field set means that the variable is not null (so
     * that it aliases with itself) but does not reach any other variable, nor
     * is reached from any variable.  As for cyclicity, the empty field set
     * means that the only cycle has length 0.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean transferNew(Quad q) {
    		Utilities.begin("PROCESSING NEW INSTRUCTION: " + q);
    		Utilities.info("OLD AV: " + GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q)));
    		AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    		Register r = ((RegisterOperand) New.getDest(q)).getRegister();
    		AbstractValue avIp = avI.clone();
    		avIp.addSinfo(r,r,FieldSet.emptyset(),FieldSet.emptyset());
    		avIp.addCinfo(r,FieldSet.emptyset());
    		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
    		showNewAV(entry,q);
    		Utilities.end("PROCESSING NEW INSTRUCTION: " + q + " - " + b);
    		return b;
    }
    
    /**
     * Adds sharing information (r,r,{},{}) and cyclicity information
     * (r,{}), where r contains the object created by {@code q}.  In the case
     * of sharing, the empty field set means that the variable is not null (so
     * that it aliases with itself) but does not reach any other variable, nor
     * is reached from any variable.  As for cyclicity, the empty field set
     * means that the only cycle has length 0.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean transferNewArray(Quad q) {
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
     * Copies the abstract information about the source variable into the
     * destination variable.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean transferMove(Quad q) {
    		Utilities.begin("PROCESSING MOVE INSTRUCTION: " + q);
    		Utilities.info("OLD AV: " + GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q)));
    		Operand op = Move.getSrc(q);
    		boolean b = false;
    		if (op instanceof AConstOperand) // null
    			b = transferSkip(q);
    		if (op instanceof RegisterOperand) {
    			AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    			AbstractValue avIp = avI.clone();
    			Register src = ((RegisterOperand) op).getRegister();
    			Register dest = ((RegisterOperand) Move.getDest(q)).getRegister();
    			if (src.isTemp() && !dest.isTemp()) avIp.moveInfo(src,dest); 
    			else avIp.copyInfo(src,dest);    			
    			b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
    		}
    		showNewAV(entry,q);
    		Utilities.end("PROCESSING MOVE INSTRUCTION: " + q + " - " + b);
    		return b;
    }
    
    /**
     * Joins the abstract information coming from both predecessor basic blocks into the successor block.
     * 
     * @param q the Quad element
     */
    protected boolean transferPhi(Quad q) {
    		Utilities.begin("PROCESSING PHI INSTRUCTION: " + q);
    		Register src1 = ((RegisterOperand) Phi.getSrc(q,0)).getRegister();
    		Register src2 = ((RegisterOperand) Phi.getSrc(q,1)).getRegister();
    		Register dest = ((RegisterOperand) Phi.getDest(q)).getRegister();
    		// this list must have two elements
    		List<BasicBlock> pbbs = q.getBasicBlock().getPredecessors();
    		BasicBlock pbb1 = pbbs.get(0);
    		BasicBlock pbb2 = pbbs.get(1);
    		AbstractValue av1 = GlobalInfo.getAV(GlobalInfo.getFinalPP(entry,pbb1));
    		AbstractValue av1_copy = av1.clone();
    		av1_copy.moveInfo(src1,dest);
    		AbstractValue av2 = GlobalInfo.getAV(GlobalInfo.getFinalPP(entry,pbb2));
    		AbstractValue av2_copy = av2.clone();
    		av2_copy.moveInfo(src2,dest);
    		// both branches are joined
    		av1_copy.update(av2_copy);
    		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),av1_copy);
    		Utilities.end("PROCESSING PHI INSTRUCTION: " + q + " - " + b);
    		return b;
    }
    
    /**
     * Transfer function for putfield.  Most of the work is done inside the 
     * AbstractValue (actually, one of its subclasses) object.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean transferPutfield(Quad q) {
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
    		AbstractValue avIp = avI.doPutfield(entry,q,v,rho,field);
    		boolean b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);
    		showNewAV(entry,q);
    		Utilities.end("PROCESSING PUTFIELD INSTRUCTION: " + q + " - " + b);
    		return b;
    }
    
    /**
     * Transfer function for return instructions.
     * - Nothing is done (just copying the information) for a RETURN_V
     * 
     * @param q
     * @return
     */
    protected boolean transferReturn(Quad q) {
		Utilities.begin("PROCESSING RETURN INSTRUCTION: " + q);
		boolean b = false;
		AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
		Utilities.info("OLD AV: " + avI);
		AbstractValue avIp = avI.clone();
		RegisterOperand x = (RegisterOperand) Return.getSrc(q);
		if (x != null) { // returns a value
			Register target = x.getRegister();
			if (!target.getType().isPrimitiveType()) {
				// information is moved, not copied, because now target is useless
				avIp.moveInfo(target,GlobalInfo.getReturnRegister(method));
			}
		}
		b = GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avIp);			
		showNewAV(entry,q);
    		Utilities.end("PROCESSING RETURN INSTRUCTION: " + q);
    		return b;
    }
    
    /**
     * Transfer function for method invocation.  Most of the work is made inside the 
     * AbstractValue (actually, one of its subclasses) object. 
     *   
     * @param q
     * @return boolean
     */
    protected boolean transferInvokeMethod(Quad q){
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
    			// the return value (null if void)
    			Register returnValue = (Invoke.getDest(q) == null) ? null : Invoke.getDest(q).getRegister();
    			
    			// 	I_s in the paper
    			AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    			Utilities.info("I_s = " + avI);
    			AbstractValue avOut = avI.doInvoke(entry,invokedEntry,q,actualParameters,returnValue);
    			b |= GlobalInfo.update(GlobalInfo.getPPAfter(entry,q),avOut);
    		} catch (NoEntryException nee) { // this should never happen
    			nee.printStackTrace();
    			return false;
    		}
		showNewAV(entry,q);
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
     * Transfer function for instructions that do not modify the abstract information.  
     * It takes the abstract value before a quad and adds it to the current abstract
     * value after the same quad.
     *  
     * @param q the Quad element
     * @return whether the abstract information after q has changed
     */
    protected boolean transferSkip(Quad q) {
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
     * In case the last Quad of the current block is a nullity or non-nullity check on some register r,
     * then information about r is removed (i.e., it is considered as null) in one of the branch. 
     * 
     * @param q the Quad element
     * @return whether the abstract information has changed somewhere (in some 
     * of the program points affected)
     */
    protected boolean transferEndBlock(Quad q) {
    		boolean b = false;
    		BasicBlock bb = q.getBasicBlock();
    		if (bb.getLastQuad() == q) { // last Quad of the current basic block
    			List<BasicBlock> bbs = bb.getSuccessors();
    			Utilities.begin("PROPAGATING AV TO SUCC BLOCKS: " + bbs);
    			LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>(bbs); 
    			AbstractValue av = GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q));
    			
    			Utilities.info("AV: " + av);
    			while (!queue.isEmpty()) {
    				BasicBlock succ = queue.removeFirst();
    				Utilities.info("BB: " + succ);
    				ProgramPoint pp = GlobalInfo.getInitialPP(entry,succ);
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
    
    /**
     * Returns true iff the Quad q corresponds to a (non-)nullity check, i.e., an AConst instruction with
     * parameters r (the register), null, and EQ (meaning that the check is for equality) or NE
     * 
     * @param q
     * @return whether q is a nullity or non-nullity check
     */
    private boolean isNullComparison(Quad q) {
    		if (q.getOperator() instanceof IntIfCmp) {
    			IntIfCmp iic = (IntIfCmp) q.getOperator();
    			if (iic instanceof IFCMP_A) {
    				Operand src2 = IFCMP_A.getSrc2(q);
    				return (src2 instanceof AConstOperand && 
    						((AConstOperand) src2).getType() instanceof jq_NullType);
    			} else return false;
    		} else return false;
    }
    
    /**
     * Returns true iff the Quad q corresponds to a object (in)equality check, i.e., an AConst instruction with
     * parameters r (the register), null, and NE (meaning that the check is for inequality)
     * 
     * @param q
     * @return whether q is a object (in)equality check
     */
    private boolean isObjectComparison(Quad q) {
    		if (q.getOperator() instanceof IntIfCmp) {
    			IntIfCmp iic = (IntIfCmp) q.getOperator();
    			if (iic instanceof IFCMP_A) {
        			Operand src1 = IFCMP_A.getSrc1(q);
        			Operand src2 = IFCMP_A.getSrc2(q);
    				return (src2 instanceof AConstOperand && 
    						((AConstOperand) src2).getType() instanceof jq_NullType &&
    						(IntIfCmp.getCond(q).getCondition() == BytecodeVisitor.CMP_NE));
    			} else return false;
    		} else return false;
    }

    /**
     * Returns the arguments itself and, if it is empty (no quads), all successors blocks and
     * successors of empty successors, recursively (using a queue)
     *  
     * @param startBlock
     * @return
     */
    private ArrayList<BasicBlock> getMyselfAndEmptySuccs(BasicBlock startBlock) {
    		ArrayList<BasicBlock> list = new ArrayList<BasicBlock>();
    		list.add(startBlock);
    		if (startBlock.getQuads().size() == 0) {
    			list.addAll(startBlock.getSuccessors());
    			LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>(startBlock.getSuccessors());
    			while (!queue.isEmpty()) {
    				BasicBlock succ = queue.removeFirst();
    				if (succ.getQuads().size() == 0) {
    					list.addAll(succ.getSuccessors());
    					queue.addAll(succ.getSuccessors());
    				}
    			}
    		}
    		return list;
    }

    private void showNewAV(Entry entry,Quad q) {
    		AbstractValue avI = GlobalInfo.getAV(GlobalInfo.getPPBefore(entry,q));
    		AbstractValue avIp = GlobalInfo.getAV(GlobalInfo.getPPAfter(entry,q));
    		if (avI.equals(avIp)) 
    			Utilities.info("NEW AV: unchanged");
    		else Utilities.info("NEW AV: " + avIp);
    }
    
}	
