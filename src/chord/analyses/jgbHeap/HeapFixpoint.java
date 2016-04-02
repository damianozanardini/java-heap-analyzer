package chord.analyses.jgbHeap;



import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
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
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Fixpoint;
import chord.analyses.damianoAnalysis.QuadQueue;
import chord.analyses.damianoAnalysis.Utilities;
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
	private jq_Method acMeth;
	
	public jq_Method getMethod() { return acMeth; }
    
    /**
     * This method processes a Quad object {@code q}, branching on the operator.
     * 
     * @param q The Quad to be processed.
     * @return whether new tuples have been added.
     */
    protected boolean process(Quad q, RelCycle cycle, RelShare share,jq_Method meth) {
    	relCycle = cycle;
    	relShare = share;
    	acMeth = meth;
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
    			System.out.println("Llamada a metodo" + q);
    			relShare.removeTuples(r,acMeth);
    			relCycle.removeTuples(r,acMeth);
    		} else   			
    			Utilities.debug("IGNORING INVOKE INSTRUCTION: " + q);
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
    	return (relShare.copyTuples(base,dest,acMeth) | relCycle.copyTuples(base,dest,acMeth));
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
    	return (relShare.moveTuples(value,base,acMeth) | relCycle.moveTuples(value,base,acMeth));
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
    	changed |= relCycle.copyTuples(base,dest,acMeth);
    	// copy self-"reachability" of dest from from cyclicity of base
    	changed |= relShare.copyTuplesFromCycle(base,dest,relCycle,acMeth);
    	// add "reachability" from the "reachability" from base, removing the field
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachingRegister(base)) {
    		FieldSet fs1 = FieldSet.removeField(p.val1,field);
    		changed |= relShare.condAdd(dest,p.val0,fs1,FieldSet.emptyset(),acMeth);
    		// the old field set is still there
    		changed |= relShare.condAdd(dest,p.val0,p.val1,FieldSet.emptyset(),acMeth);
    	}
    	// add "reachability" from the "reachability" to base, adding the field
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(base)) {
    		FieldSet fs2 = FieldSet.addField(p.val1,field);
    		changed |= relShare.condAdd(p.val0,dest,fs2,FieldSet.emptyset(),acMeth);
    	}
    	// add "reachability" to dest and sharing between r and dest from
    	// sharing between r and base 
    	for (Trio<Register,FieldSet,FieldSet> p : relShare.findTuplesByFirstRegister(base)) {
    		if (p.val1.containsOnly(field)) {
    				changed |= relShare.condAdd(p.val0,dest,p.val2,FieldSet.emptyset(),acMeth);
    			}
    		FieldSet fs3 = FieldSet.removeField(p.val1,field);
    		changed |= relShare.condAdd(base,p.val0,p.val2,fs3,acMeth);
    		changed |= relShare.condAdd(base,p.val0,p.val2,p.val1,acMeth);
    	}
    	for (Trio<Register,FieldSet,FieldSet> p : relShare.findTuplesBySecondRegister(base)) {
    		if (p.val2.containsOnly(field)) {
    				changed |= relShare.condAdd(p.val0,dest,p.val1,FieldSet.emptyset(),acMeth);
    			}
    		FieldSet fs4 = FieldSet.removeField(p.val2,field);
    		changed |= relShare.condAdd(base,p.val0,p.val1,fs4,acMeth);
    		changed |= relShare.condAdd(base,p.val0,p.val1,p.val2,acMeth);
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
    	return (relCycle.condAdd(r,FieldSet.emptyset(),acMeth) |
    			relShare.condAdd(r,r,FieldSet.emptyset(),FieldSet.emptyset(),acMeth));
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
    	return (relCycle.condAdd(r,FieldSet.emptyset(),acMeth) |
    			relShare.condAdd(r,r,FieldSet.emptyset(),FieldSet.emptyset(),acMeth));
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
    			return (relCycle.moveTuples(src,dest,acMeth) | relShare.moveTuples(src,dest,acMeth));
    		} else {
    			return (relCycle.copyTuples(src,dest,acMeth) |	relShare.copyTuples(src,dest,acMeth));
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
    	relCycle.removeTuples(destination,acMeth);
    	relShare.removeTuples(destination,acMeth);
    	return (relCycle.joinTuples(src1,src2,destination,acMeth) |
    			relShare.joinTuples(src1,src2,destination,acMeth));
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
    	
    	if (Putfield.getSrc(q) instanceof AConstOperand) return false;    	
    	if (((RegisterOperand) Putfield.getSrc(q)).getType().isPrimitiveType()) return false;
    	
    	System.out.println("BASE: " + Putfield.getBase(q));
    	
    	Register base = ((RegisterOperand) Putfield.getBase(q)).getRegister();
    	Register src = ((RegisterOperand) Putfield.getSrc(q)).getRegister();
    	jq_Field field = ((FieldOperand) Putfield.getField(q)).getField();
    	Boolean changed = false;
    	// add "reachability" created by the new path
    	for (Pair<Register,FieldSet> p1 : relShare.findTuplesByReachedRegister(base)) {
        	for (Pair<Register,FieldSet> p2 : relShare.findTuplesByReachingRegister(src)) {
    			FieldSet fs1 = FieldSet.union(p1.val1,FieldSet.addField(p2.val1,field));
    			changed |= relShare.condAdd(p1.val0,p2.val0,fs1,FieldSet.emptyset(),acMeth);
    			changed |= relShare.condAdd(p1.val0,p1.val0,fs1,fs1,acMeth);
    			for (FieldSet fs2 : relShare.findTuplesByReachingReachedRegister(src,base)) {
    				FieldSet fs3 = FieldSet.union(fs1,fs2);
    				changed |= relShare.condAdd(p1.val0,p2.val0,fs3,FieldSet.emptyset(),acMeth);
    				changed |= relShare.condAdd(p1.val0,p1.val0,fs3,fs3,acMeth);
    			}
    		}
    	}
    	// add cyclicity of variables "reaching" base
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(base)) {
    		for (FieldSet fs : relShare.findTuplesByReachingReachedRegister(src,base)) {
    			FieldSet fs0 = FieldSet.addField(fs,field);
    			changed |= relCycle.condAdd(p.val0,fs0,acMeth);
    			changed |= relShare.condAdd(p.val0,p.val0,fs0,fs0,acMeth);
    		}
    	}
    	// copy cyclicity of src into variables which "reach" base
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(base)) {
    		changed |= relCycle.copyTuples(src,p.val0,acMeth);
    		changed |= relShare.copyTuplesFromCycle(src,p.val0,relCycle,acMeth);
    	}
        // add sharing from sharing
    	for (Trio<Register,FieldSet,FieldSet> t : relShare.findTuplesByFirstRegister(src)) {
    		FieldSet fs = FieldSet.addField(t.val1,field);
    		changed |= relShare.condAdd(base,t.val0,fs,t.val2,acMeth);
    	}
    	for (Trio<Register,FieldSet,FieldSet> t : relShare.findTuplesBySecondRegister(src)) {
    		FieldSet fs = FieldSet.addField(t.val2,field);
    		changed |= relShare.condAdd(t.val0,base,t.val1,fs,acMeth);
    	}
    	return changed;
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
    	//RelUseDef relUseDef = (RelUseDef) ClassicProject.g().getTrgt("UseDef");
    	//relUseDef.load();
    	//List<Quad> l = relUseDef.getByFirstArg(q);
    	//queue.addList(l);
    }

	public void save() {
		relShare.save();
		relCycle.save();
	}    
}	
