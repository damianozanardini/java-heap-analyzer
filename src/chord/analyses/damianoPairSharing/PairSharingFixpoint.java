package chord.analyses.damianoPairSharing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.PrintCFG;
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
import chord.analyses.damianoAnalysis.ParseInputLineException;
import chord.analyses.damianoAnalysis.QuadQueue;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.field.DomF;
import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;


/**
 * Implementation of the fixpoint.  It adds tuples to the sharing relation.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
public class PairSharingFixpoint extends Fixpoint {
    /**
     * The queue for implementing the fixpoint.
     */
	private QuadQueue queue;
		
	/**
	 * The method to be analyzed (currently, the analysis is intraprocedural).
	 */
	private jq_Method an_method;
	
	/**
	 * The sharing relation.
	 */
	private RelPairSharing relShare;

	public RelPairSharing getRelShare() {
		return relShare;
	}

	private ArrayList<Pair<Register,Register>> outShare = null;
	
	public void parseInputLine(String line0) throws ParseInputLineException {
		Utilities.out("  READING LINE '" + line0 +"'...");
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return; // empty line
		String[] tokens = line.split(" ");
		// WARNING: this line should come before the others in the input file;
		// otherwise, some info won't be correctly stored
		if (tokens[0].equals("M")) {
			setMethod(tokens[1]);
			return;
		}
		if (tokens[0].equals("pairSharing")) {
			if (tokens[1].equals("S")) { // it is a sharing statement
				try {
					Register r1 = RegisterManager.getRegFromInputToken(getMethod(),tokens[2]);
					Register r2 = RegisterManager.getRegFromInputToken(getMethod(),tokens[3]);
					relShare.condAdd(getFirstQuad(),r1,r2);
				} catch (NumberFormatException e) {
					System.out.println("    ERROR: incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("    ERROR: illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("    ERROR: something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
				return;
			}
			if (tokens[1].equals("S?")) { // it is a sharing statement on output
				try {
					Register r1 = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[2]);
					Register r2 = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[3]);
					outShare.add(new Pair<Register,Register>(r1,r2));
				} catch (NumberFormatException e) {
					System.out.println("    ERROR: incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("    ERROR: illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("    ERROR: something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
			}
		}
	}
	
	/**
	 * This method inits/loads the relations and sets the input, if it is specified.
	 */
	public void init() {
		relShare = (RelPairSharing) ClassicProject.g().getTrgt("PairShare");
		relShare.run();
		relShare.load();
		// TODO tuples should not be needed
		relShare.tuples = new ArrayList<Trio<Quad,Register,Register>>();
		
		outShare = new ArrayList<Pair<Register,Register>>();
		try {
			BufferedReader br;
			br = new BufferedReader(new FileReader(Config.workDirName + "/input"));
			readInputFile(br);
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: file " + Config.workDirName + "/input" + " not found, assuming");
			System.out.println(" - method to be analyzed: main method");
			System.out.println(" - empty input");
			setMethod();
		}
		
		// debug-only
		ControlFlowGraph cfg = CodeCache.getCode(getMethod());
		Quad first = cfg.entry().getQuad(0);
		System.out.println("********************" + first);
		new PrintCFG().visitCFG(cfg);
		
		// outputting source-code variables corresponding to registers
		RegisterManager.printVarRegMap(getMethod());
	}
	
	/**
	 * This method initializes the Quad queue and runs the fixpoint.
	 */
    public void run() {
        jq_Method meth = getMethod();
        
    	// initializing the queue
    	queue = new QuadQueue(meth,QuadQueue.FORWARD);
       	
    	// implementation of the fixpoint
    	boolean needNextIteration;
    	do {
    		needNextIteration = false;
    		for (Quad q : queue) needNextIteration |= process(q);
    	} while (needNextIteration);
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
    			relShare.removeTuples(r);
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
    	return copyFW(q,base,dest);
    }
    
    protected boolean copyFW(Quad q) {
    	boolean changed = false;
    	for (Quad qq : getNextQuads(q)) {
    		changed |= (relShare.copyTuples(q,qq));
    	}
    	return changed;
    }
    
    protected boolean copyFW(Quad q, Register src, Register dest) {
    	boolean changed = false;
    	for (Quad qq : getNextQuads(q)) {
    		changed |= (relShare.copyTuples(q,qq,src,dest));
    	}
    	return changed;
    }

    protected boolean copyBW(Quad q, Register src, Register dest) {
    	boolean changed = false;
    	for (Quad qq : getPrevQuads(q)) {
    		changed |= (relShare.copyTuples(q,qq,src,dest));
    	}
    	return changed;
    }

    protected boolean copyBW(Quad q) {
    	boolean changed = false;
    	for (Quad qq : getPrevQuads(q)) {
    		changed |= (relShare.copyTuples(q,qq));
    	}
    	return changed;
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
    	return copyFW(q,value,base);
    }
    
    /**
     * This method adds a pair between the source and destination variable
     *
     * @param q The Quad to be processed.
     */
    protected boolean processGetfield(Quad q) {
    	// TODO transitive closure?
    	Utilities.debug("PROCESSING GETFIELD INSTRUCTION: " + q);
    	if (((RegisterOperand) Getfield.getDest(q)).getType().isPrimitiveType()) return false;
    	Register base = ((RegisterOperand) Getfield.getBase(q)).getRegister();
    	Register dest = ((RegisterOperand) Getfield.getDest(q)).getRegister();
    	Boolean changed = false;
    	List<Register> sharingWithBase = relShare.findTuplesByRegister(q,base);
    	changed |= copyFW(q);
		for (Quad qq : getNextQuads(q)) {
			for (Register r : sharingWithBase) {
				List<Register> sharingWithDest = relShare.findTuplesByRegister(qq,dest);
				for (Register rr : sharingWithDest)
    			changed |= relShare.condAdd(qq,r,rr);
    		}
    	}
    	return changed;
    }

    /**
     * This method adds a sharing tuple (r,r), where r contains the object
     * created by {@code q}.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processNew(Quad q) {
    	Utilities.debug("PROCESSING NEW INSTRUCTION: " + q);
    	Register r = ((RegisterOperand) New.getDest(q)).getRegister();
    	return (relShare.condAdd(q,r,r));
    }
    
    /**
     * This method adds a sharing tuple (r,r), where r contains the object
     * created by {@code q}.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processNewArray(Quad q) {
    	Utilities.debug("PROCESSING NEWARRAY INSTRUCTION: " + q);
    	Register r = ((RegisterOperand) NewArray.getDest(q)).getRegister();
    	return (relShare.condAdd(q,r,r));
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
    		return (copyFW(q,src,dest));
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
    	boolean changed = false;
    	for (Quad qq : getNextQuads(q)) {
    		changed |= (relShare.joinTuples(q,qq,src1,src2,destination));
    	}
    	return changed;
    }
    
    /**
     * This method takes a putfield Quad and accounts for new paths in the heap
     * which are created by the heap update {@code base}.f={@code src}.
     * Concretely, it adds a new sharing tuple (base,src). 
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processPutfield(Quad q) {
    	Utilities.debug("PROCESSING PUTFIELD INSTRUCTION: " + q);
    	if (((RegisterOperand) Putfield.getSrc(q)).getType().isPrimitiveType()) return false;
    	Register base = ((RegisterOperand) Putfield.getBase(q)).getRegister();
    	Register src = ((RegisterOperand) Putfield.getSrc(q)).getRegister();
    	Boolean changed = false;
    	List<Register> sharingWithSrc = relShare.findTuplesByRegister(q,src);
    	changed |= copyFW(q);
		for (Quad qq : getNextQuads(q)) {
			for (Register r : sharingWithSrc) {
				List<Register> sharingWithBase = relShare.findTuplesByRegister(qq,base);
				for (Register rr : sharingWithBase)
    			changed |= relShare.condAdd(qq,r,rr);
    		}
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
    
    public void printOutput() {
    	for (Pair<Register,Register> p : outShare)
    		relShare.askForS(getMethod(),p.val0,p.val1);
    }

	public void save() {
		relShare.save();
	}    
}	
