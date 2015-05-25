package chord.analyses.damianoCyclicity;

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
 * Implementation of the fixpoint.  It adds tuples to sharing
 * and cyclicity relation.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
public class CyclicityFixpoint extends Fixpoint {
    /**
     * The queue for implementing the fixpoint.
     */
	private QuadQueue queue;
	
	private AccumulatedTuples accumulatedTuples;
	
	/**
	 * The method to be analyzed (currently, the analysis is intraprocedural).
	 */
	private jq_Method an_method;
	
	/**
	 * The sharing relation.
	 */
	private RelShare relShare;

	public RelShare getRelShare() {
		return relShare;
	}

	/**
	 * The cyclicity relation.
	 */
	private RelCycle relCycle;
	
	public RelCycle getRelCycle() {
		return relCycle;
	}

	private ArrayList<Pair<Register,Register>> outShare = null;
	private ArrayList<Register> outCycle = null;
	
	protected void setTrackedFields(BufferedReader br) {
		Boolean x = false;
		try {
			String line = br.readLine();
			while (line != null && x==false) {
				try	{
					x |= parseFLine(line);
				} catch (ParseInputLineException e) { }
				line = br.readLine();
			}
			if (x == false) { // no F line parsed successfully
				DomAbsF absF = (DomAbsF) ClassicProject.g().getTrgt("AbsF");
				absF.run();
			}
			br.close();
		} catch (IOException e) {}
	}
	
	protected Boolean parseFLine(String line0) throws ParseInputLineException {
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return false; // empty line
		String[] tokens = line.split(" ");
		if (tokens[0].equals("F")) { // it is the list of fields to be tracked explicitly
			try {
				List<jq_Field> l = parseFieldsList(tokens,1,tokens.length);
				DomAbsF absF = (DomAbsF) ClassicProject.g().getTrgt("AbsF");
				absF.trackedFields = l;
				absF.run();
				System.out.println("EXPLICITLY TRACKING FIELDS " + l);
			} catch (ParseFieldException e) {
				if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
					System.out.println("ERROR: could not find field " + e.getField());
				if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
					System.out.println("ERROR: could not resolve field (multiple choices)" + e.getField());
				throw new ParseInputLineException(line0);
			}
			return true;
		}
		return false;
	}	
	
	/** 
	 * This method parses a String into a statement.  Currently, it checks
	 * that the statement is a sharing or cyclicity one (as indicated by
	 * the letter "S" or "C" in the first place). A line takes the form of a list
	 * of space-separated tokens S V1 F1 ... FK / G1/GL V2 where
	 * <ul>
	 * <li> S indicates that it is a sharing statement
	 * <li> V1 is a number indicating the position of the first register
	 * in the sequence of local variables (e.g., for an instance method, 0 is this)
	 * <li> each Fi is a field (either complete or partial) identifier
	 * <li> / is a separator
	 * <li> each Gj is a field (either complete or partial) identifier
	 * <li> V2 is a number indicating the position of the second register
	 * in the sequence of local variables
	 * </ul>
	 * or C V F1 ... FK where
	 * <ul>
	 * <li> C indicates that it is a cyclicity statement
	 * <li> V is a number indicating the position of the source register
	 * in the sequence of local variables (e.g., for an instance method, 0 is this)
	 * <li> each Fi is a field (either complete or partial) identifier
	 * </ul>
	 * A well-formed input string corresponds to a tuple in the relation; if 
	 * parsing is successful, the tuple is added to the relation.
	 * Line comments start with '%', as in latex.
	 * 
	 * @param line0 The input string.
	 * @throws ParseInputLineException if the input cannot be parsed successfully.
	 */
	public void parseInputLine(String line0) throws ParseInputLineException {
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return; // empty line
		String[] tokens = line.split(" ");
		if (tokens[0].equals("M")) { // it is a sharing statement
			setMethod(tokens[1]);
			return;
		}	
		if (tokens[0].equals("S")) { // it is a sharing statement
			try {
				Register r1 = RegisterManager.getRegFromInputToken(getMethod(),tokens[1]);
				Register r2 = RegisterManager.getRegFromInputToken(getMethod(),tokens[tokens.length-1]);
				boolean barFound = false;
				int i;
				for (i = 2; i < tokens.length-1 && !barFound; i++) {
					if (tokens[i].equals("/")) barFound = true;
				}
				if (!barFound) {
					System.out.println("ERROR: separating bar / not found... ");
					throw new ParseInputLineException(line0);
				}
				FSet fset1 = parseFieldsFSet(tokens,2,i-1);
				FSet fset2 = parseFieldsFSet(tokens,i,tokens.length-1);
				relShare.condAdd(r1,r2,fset1,fset2);
			} catch (NumberFormatException e) {
				System.out.println("ERROR: incorrect register representation " + e);
				throw new ParseInputLineException(line0);
			} catch (IndexOutOfBoundsException e) {
				System.out.println("ERROR: illegal register " + e);
				throw new ParseInputLineException(line0);
			} catch (ParseFieldException e) {
				if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
					System.out.println("ERROR: could not find field " + e.getField());
				if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
					System.out.println("ERROR: could not resolve field (multiple choices)" + e.getField());
				throw new ParseInputLineException(line0);
			} catch (RuntimeException e) {
				System.out.println("ERROR: something went wrong... " + e);
				throw new ParseInputLineException(line0);
			}
			return;
		}
		if (tokens[0].equals("C")) { // it is a cyclicity statement
			try {
				int idx = Integer.parseInt(tokens[1]); // index of the register
				Register r = RegisterManager.getRegFromNumber(getMethod(),idx);
				FSet fset = parseFieldsFSet(tokens,2,tokens.length);
				relCycle.condAdd(r,fset);
			} catch (NumberFormatException e) {
				System.out.println("ERROR: incorrect register representation " + e);
				throw new ParseInputLineException(line0);
			} catch (IndexOutOfBoundsException e) {
				System.out.println("ERROR: illegal register " + e);
				throw new ParseInputLineException(line0);
			} catch (ParseFieldException e) {
				if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
					System.out.println("ERROR: could not find field " + e.getField());
				if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
					System.out.println("ERROR: could not resolve field (multiple choices)" + e.getField());
				throw new ParseInputLineException(line0);
			} catch (RuntimeException e) {
				System.out.println("ERROR: something went wrong... " + e);
				throw new ParseInputLineException(line0);
			}
			return;
		}
		if (tokens[0].equals("S?")) { // it is a sharing statement on output
			try {
				Register r1 = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[1]);
				Register r2 = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[2]);				
				outShare.add(new Pair<Register,Register>(r1,r2));
			} catch (NumberFormatException e) {
				System.out.println("ERROR: incorrect register representation " + e);
				throw new ParseInputLineException(line0);
			} catch (IndexOutOfBoundsException e) {
				System.out.println("ERROR: illegal register " + e);
				throw new ParseInputLineException(line0);
			} catch (RuntimeException e) {
				System.out.println("ERROR: something went wrong... " + e);
				throw new ParseInputLineException(line0);
			}
		}
		if (tokens[0].equals("C?")) { // it is a cyclicity statement on output
			try {
				Register r = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[1]);
				outCycle.add(r);
			} catch (NumberFormatException e) {
				System.out.println("ERROR: incorrect register representation " + e);
				throw new ParseInputLineException(line0);
			} catch (IndexOutOfBoundsException e) {
				System.out.println("ERROR: illegal register " + e);
				throw new ParseInputLineException(line0);
			} catch (RuntimeException e) {
				System.out.println("ERROR: something went wrong... " + e);
				throw new ParseInputLineException(line0);
			}
			return;
		}
	}

	/**
	 * Scans an array {@code tokens} of {@code String} tokens from index {@code idx1}
	 * to {@code idx2}; for each of them, retrieves a field, and put them all together
	 * is an {@code FSet} object.
	 *  
	 * @param tokens The array of String.
	 * @param idx1 The index of the first relevant token. 
	 * @param idx2 The index (plus 1) of the last relevant token. 
	 * @return The {@code FSet} object representing all parsed fields. 
	 * @throws ParseFieldException if some field name cannot be parsed.
	 */
	protected FSet parseFieldsFSet(String[] tokens, int idx1, int idx2) throws ParseFieldException {
		FSet fset = FSet.emptyset();
		for (int i=idx1; i<idx2; i++) {
			try {
				jq_Field f = parseField(tokens[i]);
				fset = FSet.addField(fset,f);
			} catch (ParseFieldException e) {
				throw e;
			}
		}
		return fset;
	}
	
	/**
	 * Scans an array {@code tokens} of {@code String} tokens from index {@code idx1}
	 * to {@code idx2}; for each of them, retrieves a field, and returns a field list. 
	 * @param tokens The array of String.
	 * @param idx1 The index of the first relevant token. 
	 * @param idx2 The index (plus 1) of the last relevant token. 
	 * @return The list of parsed fields. 
	 * @throws ParseFieldException if some field name cannot be parsed.
	 */
	protected List<jq_Field> parseFieldsList(String[] tokens, int idx1, int idx2) throws ParseFieldException {
		List<jq_Field> l = new ArrayList<jq_Field>();
		for (int i=idx1; i<idx2; i++) {
			try {
				jq_Field f = parseField(tokens[i]);
				if (f!=null) l.add(f);
			} catch (ParseFieldException e) {
				throw e;
			}
		}
		return l;
	}
	
	/**
	 * Reads a field identifier and returns a field.
	 * The identifier must be a suffix of the complete field description
	 * (<full_class_name>.<field_name>).
	 * @param str The field identifier.
	 * @return the parsed field.
	 * @throws ParseFieldException if either
	 * <ul>
	 * <li> no field corresponds to {@code str}; or
	 * <li> {@code str} is ambiguous (i.e., more than one field correspond to it).
	 * </ul>
	 */
	protected jq_Field parseField(String str) throws ParseFieldException {
		List<jq_Field> list = new ArrayList<jq_Field>();
		DomF fields = (DomF) ClassicProject.g().getTrgt("F");
		for (int i=1; i<fields.size(); i++) {
			jq_Field f = (jq_Field) fields.get(i);
			if (f!=null) {
				String completeName = f.getClass() + "." + f.getName();
				if (completeName.endsWith(str)) list.add(f);
			}
		}
		switch (list.size()) {
		case 0:
			throw new ParseFieldException(ParseFieldException.FIELDNOTFOUND,str);
		case 1:
			return list.get(0);
		default:
			throw new ParseFieldException(ParseFieldException.MULTIPLEFIELDS,str);
		}
	}
	
	/**
	 * This method inits/loads the relations and sets the input, if it is specified.
	 */
	public void init() {
		accumulatedTuples = new AccumulatedTuples();
		relShare = (RelShare) ClassicProject.g().getTrgt("Share");
		relShare.run();
		relShare.load();
		relShare.accumulatedTuples = accumulatedTuples;
		relCycle = (RelCycle) ClassicProject.g().getTrgt("Cycle");
		relCycle.run();
		relCycle.load();
		relCycle.accumulatedTuples = accumulatedTuples;
		outShare = new ArrayList<Pair<Register,Register>>();
		outCycle = new ArrayList<Register>();
		try {
			BufferedReader br;
			br = new BufferedReader(new FileReader(Config.workDirName + "/input"));
			readInputFile(br);
			br = new BufferedReader(new FileReader(Config.workDirName + "/input")); // back to the beginning
			setTrackedFields(br);
			DomFSet domFSet = (DomFSet) ClassicProject.g().getTrgt("FSet");
			domFSet.run();
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: file " + Config.workDirName + "/input" + " not found, assuming");
			System.out.println(" - method to be analyzed: main method");
			System.out.println(" - all fields tracked explicitly");
			System.out.println(" - empty input");
			setMethod();
			DomFSet domFSet = (DomFSet) ClassicProject.g().getTrgt("FSet");
			domFSet.fill();
		}
		
		// debug-only
		ControlFlowGraph cfg = CodeCache.getCode(getMethod());
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
    			relCycle.removeTuples(r);
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
    	return (relShare.copyTuples(base,dest) | relCycle.copyTuples(base,dest));
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
    	return (relShare.moveTuples(value,base) | relCycle.moveTuples(value,base));
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
    	changed |= relCycle.copyTuples(base,dest);
    	// copy self-"reachability" of dest from from cyclicity of base
    	changed |= relShare.copyTuplesFromCycle(base,dest,relCycle);
    	// add "reachability" from the "reachability" from base, removing the field
    	for (Pair<Register,FSet> p : relShare.findTuplesByReachingRegister(base)) {
    		FSet fs1 = FSet.removeField(p.val1,field);
    		changed |= relShare.condAdd(dest,p.val0,fs1,FSet.emptyset());
    		// the old field set is still there
    		changed |= relShare.condAdd(dest,p.val0,p.val1,FSet.emptyset());
    	}
    	// add "reachability" from the "reachability" to base, adding the field
    	for (Pair<Register,FSet> p : relShare.findTuplesByReachedRegister(base)) {
    		FSet fs2 = FSet.addField(p.val1,field);
    		changed |= relShare.condAdd(p.val0,dest,fs2,FSet.emptyset());
    	}
    	// add "reachability" to dest and sharing between r and dest from
    	// sharing between r and base 
    	for (Trio<Register,FSet,FSet> p : relShare.findTuplesByFirstRegister(base)) {
    		if (p.val1.containsOnly(field)) {
    				changed |= relShare.condAdd(p.val0,dest,p.val2,FSet.emptyset());
    			}
    		FSet fs3 = FSet.removeField(p.val1,field);
    		changed |= relShare.condAdd(base,p.val0,p.val2,fs3);
    		changed |= relShare.condAdd(base,p.val0,p.val2,p.val1);
    	}
    	for (Trio<Register,FSet,FSet> p : relShare.findTuplesBySecondRegister(base)) {
    		if (p.val2.containsOnly(field)) {
    				changed |= relShare.condAdd(p.val0,dest,p.val1,FSet.emptyset());
    			}
    		FSet fs4 = FSet.removeField(p.val2,field);
    		changed |= relShare.condAdd(base,p.val0,p.val1,fs4);
    		changed |= relShare.condAdd(base,p.val0,p.val1,p.val2);
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
    	return (relCycle.condAdd(r,FSet.emptyset()) |
    			relShare.condAdd(r,r,FSet.emptyset(),FSet.emptyset()));
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
    	return (relCycle.condAdd(r,FSet.emptyset()) |
    			relShare.condAdd(r,r,FSet.emptyset(),FSet.emptyset()));
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
    			return (relCycle.moveTuples(src,dest) | relShare.moveTuples(src,dest));
    		} else {
    			return (relCycle.copyTuples(src,dest) |	relShare.copyTuples(src,dest));
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
    	relCycle.removeTuples(destination);
    	relShare.removeTuples(destination);
    	return (relCycle.joinTuples(src1,src2,destination) |
    			relShare.joinTuples(src1,src2,destination));
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
    	if (((RegisterOperand) Putfield.getSrc(q)).getType().isPrimitiveType()) return false;
    	Register base = ((RegisterOperand) Putfield.getBase(q)).getRegister();
    	Register src = ((RegisterOperand) Putfield.getSrc(q)).getRegister();
    	jq_Field field = ((FieldOperand) Putfield.getField(q)).getField();
    	Boolean changed = false;
    	// add "reachability" created by the new path
    	for (Pair<Register,FSet> p1 : relShare.findTuplesByReachedRegister(base)) {
        	for (Pair<Register,FSet> p2 : relShare.findTuplesByReachingRegister(src)) {
    			FSet fs1 = FSet.union(p1.val1,FSet.addField(p2.val1,field));
    			changed |= relShare.condAdd(p1.val0,p2.val0,fs1,FSet.emptyset());
    			changed |= relShare.condAdd(p1.val0,p1.val0,fs1,fs1);
    			for (FSet fs2 : relShare.findTuplesByReachingReachedRegister(src,base)) {
    				FSet fs3 = FSet.union(fs1,fs2);
    				changed |= relShare.condAdd(p1.val0,p2.val0,fs3,FSet.emptyset());
    				changed |= relShare.condAdd(p1.val0,p1.val0,fs3,fs3);
    			}
    		}
    	}
    	// add cyclicity of variables "reaching" base
    	for (Pair<Register,FSet> p : relShare.findTuplesByReachedRegister(base)) {
    		for (FSet fs : relShare.findTuplesByReachingReachedRegister(src,base)) {
    			FSet fs0 = FSet.addField(fs,field);
    			changed |= relCycle.condAdd(p.val0,fs0);
    			changed |= relShare.condAdd(p.val0,p.val0,fs0,fs0);
    		}
    	}
    	// copy cyclicity of src into variables which "reach" base
    	for (Pair<Register,FSet> p : relShare.findTuplesByReachedRegister(base)) {
    		changed |= relCycle.copyTuples(src,p.val0);
    		changed |= relShare.copyTuplesFromCycle(src,p.val0,relCycle);
    	}
        // add sharing from sharing
    	for (Trio<Register,FSet,FSet> t : relShare.findTuplesByFirstRegister(src)) {
    		FSet fs = FSet.addField(t.val1,field);
    		changed |= relShare.condAdd(base,t.val0,fs,t.val2);
    	}
    	for (Trio<Register,FSet,FSet> t : relShare.findTuplesBySecondRegister(src)) {
    		FSet fs = FSet.addField(t.val2,field);
    		changed |= relShare.condAdd(t.val0,base,t.val1,fs);
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
    		accumulatedTuples.askForFS(getMethod(),p.val0,p.val1);
    	for (Register r : outCycle)
    		accumulatedTuples.askForC(getMethod(),r);
    }    
}	
