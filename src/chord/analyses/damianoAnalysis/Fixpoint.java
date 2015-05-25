package chord.analyses.damianoAnalysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joeq.Class.jq_Method;
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
import joeq.Compiler.Quad.Operator.Jsr;
import joeq.Compiler.Quad.Operator.LookupSwitch;
import joeq.Compiler.Quad.Operator.MemLoad;
import joeq.Compiler.Quad.Operator.MemStore;
import joeq.Compiler.Quad.Operator.Monitor;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.NullCheck;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Putfield;
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
import chord.analyses.damianoAbstractSlicing.Agreement;
import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.project.Config;

/**
 * Implementation of the generic fixpoint.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
public class Fixpoint {

	/**
	 * The queue for implementing the fixpoint.
	 */
	protected QuadQueue queue;
	
	/**
	 * The method to be analyzed (currently, the analysis is intraprocedural).
	 */
	protected jq_Method an_method;
		
	/**
	 * Sets the method to be analyzed (default is main).
	 */
	protected void setMethod() {	
		Utilities.debug("setMethod: SETTING METHOD TO DEFAULT: main");
		setMethod(Program.g().getMainMethod());
	}
			
	/**
	 * Sets the method to be analyzed (default is main).
	 * @param m The method to be analyzed.
	 */
	protected void setMethod(jq_Method m) {
		Utilities.debug("setMethod_aux: SETTING METHOD FROM jq_Method OBJECT: " + m);
		if (m == null) an_method = Program.g().getMainMethod();
		else an_method = m;
		Utilities.debug("setMethod_aux: METHOD FINALLY SET TO " + an_method);
	}
			
	/**
	 * Sets the method to be analyzed by searching a method whose signature
	 * is compatible with {@code str}.  If no method is found, or more than
	 * one is compatible, then the method to be analyzed is set to main. 
	 * @param str
	 */
	protected void setMethod(String str) {
		Utilities.debug("setMethod_aux: SETTING METHOD FROM STRING: " + str);
		List<jq_Method> list = new ArrayList<jq_Method>();
		DomM methods = (DomM) ClassicProject.g().getTrgt("M");
		for (int i=0; i<methods.size(); i++) {
			jq_Method m = (jq_Method) methods.get(i);
			if (m!=null) {
				if (m.getName().toString().equals(str)) {
					list.add(m);
				}
			}
		}	
		if (list.size()==1) setMethod(list.get(0));
		else setMethod();
	}
		
	/**
	 * Gets the method to be analyzed.
	 * 
	 * @return the method to be analyzed.
	 */
	public jq_Method getMethod () {
		return an_method;
	}

	protected Boolean parseMLine(String line0) {
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return false; // empty line
		String[] tokens = line.split(" ");
		if (tokens[0].equals("M")) { // it is the method to be analyzed
			setMethod(tokens[1]);
			return true;
		}
		return false;
	}
		
	public void init() {
		Utilities.debug("*** ===============================================================");
		Utilities.debug("*** BEGIN INITIALIZATION");
				
		try {
			BufferedReader br;
			br = new BufferedReader(new FileReader(Config.workDirName + "/input")); // back to the beginning
			readInputFile(br);
		} catch (FileNotFoundException e) {
			setMethod();
		}
		// initializing the queue (default is a forward analysis)
        jq_Method meth = getMethod();
        Utilities.out("MAIN METHOD: " + meth);
    	queue = new QuadQueue(meth,QuadQueue.FORWARD);
    	Utilities.out("*** END INITIALIZATION");
	}

	public void readInputFile(BufferedReader br) {
		Utilities.out("READING FROM INPUT FILE...");
		try {
			String line = br.readLine();
			while (line != null) {
				try {
					parseInputLine(line);
				} catch (ParseInputLineException e) {
					Utilities.out("IMPOSSIBLE TO READ LINE: " + e);
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			Utilities.out("READING FROM INPUT FROM FILE COULD NOT BE DONE");
		}
		Utilities.out("READING INPUT FROM FILE DONE");
	}

	/**
	 * Minimal initialization in case the input file is not found
	 */
	public void readInputFile() {
		setMethod();
	}
		
	public void parseInputLine(String s) throws ParseInputLineException { }
	
	/**
	 * This method initializes the Quad queue and runs the fixpoint.
	 */
	public void run() {
		Utilities.out("*** ===============================================================");
		Utilities.out("*** BEGIN ANALYSIS");
		
		// implementation of the fixpoint
		int numberOfIterations = 0;
		boolean needNextIteration;
		do {
			needNextIteration = false;
			Utilities.debug("*** ---------------------------------------------------------------");
			Utilities.debug("*** ITERATION NUMBER " + numberOfIterations);
			for (Quad q : queue) needNextIteration |= process(q);
			numberOfIterations++;
		} while (needNextIteration);
		Utilities.out("*** END ANALYSIS");
	}
	
	/**
	 * This method processes a Quad object {@code q}, branching on the operator.
	 * 
	 * @param q The Quad to be processed.
	 * @return whether the question list has been updated.
	 */
	protected boolean process(Quad q) {
		Utilities.debug("-------------------------------------------------------------------");
		Utilities.debug("PROCESSING QUAD..." + q);
		Operator operator = q.getOperator();
		if (operator instanceof ALength) { return processALength(q); }
		if (operator instanceof ALoad) { return processALoad(q); }
		if (operator instanceof AStore) { return processAStore(q); }
		if (operator instanceof Binary) { return processBinary(q); }
		if (operator instanceof BoundsCheck) { return processBoundsCheck(q); }
		if (operator instanceof Branch) {
			if (operator instanceof Goto) { return processGoto(q); }
			if (operator instanceof IntIfCmp) { return processIntIfCmp(q); }
			if (operator instanceof Jsr) { return processJsr(q); }
			if (operator instanceof LookupSwitch) { return processLookupSwitch(q); }
			if (operator instanceof Ret) { return processRet(q); }
			if (operator instanceof TableSwitch) { return processTableSwitch(q); }		
			return processBranch(q);
		}
		if (operator instanceof CheckCast) { return processCheckCast(q); }
		if (operator instanceof Getfield) { return processGetfield(q); }
		if (operator instanceof Getstatic) { return processGetStatic(q); }
		if (operator instanceof InstanceOf) { return processInstanceOf(q); }
		if (operator instanceof Invoke) { return processInvoke(q); }
		if (operator instanceof MemLoad) { return processMemLoad(q); }
		if (operator instanceof MemStore) { return processMemStore(q); }
		if (operator instanceof Monitor) { return processMonitor(q); }
		if (operator instanceof Move) { return processMove(q); }
		if (operator instanceof MultiNewArray) { return processMultiNewArray(q); }
		if (operator instanceof New) { return processNew(q); }
		if (operator instanceof NewArray) {	return processNewArray(q); }
		if (operator instanceof NullCheck) { return processNullCheck(q); }
		if (operator instanceof Phi) { return processPhi(q); }
		if (operator instanceof Putfield) { return processPutfield(q); }
		if (operator instanceof Putstatic) { return processPutStatic(q); }
		if (operator instanceof Return) { return processReturn(q); }
		if (operator instanceof Special) { return processSpecial(q); }
		if (operator instanceof StoreCheck) { return processStoreCheck(q); }
		if (operator instanceof Unary) { return processUnary(q); }
		if (operator instanceof ZeroCheck) { return processZeroCheck(q); }
		Utilities.debug("CANNOT DEAL WITH QUAD" + q);
		return false;
	}

	protected boolean processALength(Quad q) {
		Utilities.debug("IGNORING ALENGTH INSTRUCTION: " + q);
		return false;
	}
	    
	protected boolean processALoad(Quad q) {
		Utilities.debug("IGNORING ALOAD INSTRUCTION: " + q);
		return false;
	}

	protected boolean processAStore(Quad q) {
		Utilities.debug("IGNORING ASTORE INSTRUCTION: " + q);
		return false;
	}

	protected boolean processBinary(Quad q) {
		Utilities.debug("IGNORING BINARY INSTRUCTION: " + q);
		return false;
	}

	protected boolean processBoundsCheck(Quad q) {
		Utilities.debug("IGNORING BOUNDSCHECK INSTRUCTION: " + q);
		return false;
	}

	protected boolean processBranch(Quad q) {
		Utilities.debug("IGNORING BRANCH INSTRUCTION: " + q);
		return false;
	}

	protected boolean processCheckCast(Quad q) {
		Utilities.debug("IGNORING CHECKCAST INSTRUCTION: " + q);
		return false;
	}

	protected boolean processGetfield(Quad q) {
		Utilities.debug("IGNORING GETFIELD INSTRUCTION: " + q);
		return false;
	}

	protected boolean processGetStatic(Quad q) {
		Utilities.debug("IGNORING GETSTATIC INSTRUCTION: " + q);
		return false;
	}

	protected boolean processGoto(Quad q) {
		Utilities.debug("IGNORING GOTO INSTRUCTION: " + q);
		return false;
	}
	
	protected boolean processInstanceOf(Quad q) {
		Utilities.debug("IGNORING INSTANCEOF INSTRUCTION: " + q);
		return false;
    }
	
	protected boolean processIntIfCmp(Quad q) {
		Utilities.debug("IGNORING INTIFCMP INSTRUCTION: " + q);
		return false;
	}

	protected boolean processInvoke(Quad q) {
		Utilities.debug("IGNORING INVOKE INSTRUCTION: " + q);
		return false;
	}

	protected boolean processJsr(Quad q) {
		Utilities.debug("IGNORING JSR INSTRUCTION: " + q);
		return false;
	}
	
	protected boolean processLookupSwitch(Quad q) {
		Utilities.debug("IGNORING LOOKUPSWITCH INSTRUCTION: " + q);
		return false;
	}

	protected boolean processMemLoad(Quad q) {
		Utilities.debug("IGNORING MEMLOAD INSTRUCTION: " + q);
		return false;
	}

	protected boolean processMemStore(Quad q) {
		Utilities.debug("IGNORING MEMSTORE INSTRUCTION: " + q);
		return false;
	}
	
	protected boolean processMonitor(Quad q) {
		Utilities.debug("IGNORING MONITOR INSTRUCTION: " + q);
		return false;
	}
	
	protected boolean processMove(Quad q) {
		Utilities.debug("IGNORING MOVE INSTRUCTION: " + q);
		return false;
	}
	
	protected boolean processMultiNewArray(Quad q) {
		Utilities.debug("IGNORING MULTINEWARRAY INSTRUCTION: " + q);
		return false;
	}

	protected boolean processNew(Quad q) {
		Utilities.debug("IGNORING NEW INSTRUCTION: " + q);
		return false;
	}
	    
	protected boolean processNewArray(Quad q) {
		Utilities.debug("IGNORING NEWARRAY INSTRUCTION: " + q);
		return false;
	}

	protected boolean processNullCheck(Quad q) {
		Utilities.debug("IGNORING NULLCHECK INSTRUCTION: " + q);
		return false;
	}
	
	protected boolean processPhi(Quad q) {
		Utilities.debug("IGNORING PHI INSTRUCTION: " + q);
		return false;
	}
	    
	protected boolean processPutfield(Quad q) {
		Utilities.debug("IGNORING PUTFIELD INSTRUCTION: " + q);
		return false;
	}

	protected boolean processPutStatic(Quad q) {
		Utilities.debug("IGNORING PUTSTATIC INSTRUCTION: " + q);
		return false;
	}

	protected boolean processRet(Quad q) {
		Utilities.debug("IGNORING RET INSTRUCTION: " + q);
		return false;
	}

	protected boolean processReturn(Quad q) {
		Utilities.debug("IGNORING RETURN INSTRUCTION: " + q);
		return false;
	}
	
	protected boolean processSpecial(Quad q) {
		Utilities.debug("IGNORING SPECIAL INSTRUCTION: " + q);
		return false;
	}

	protected boolean processStoreCheck(Quad q) {
		Utilities.debug("IGNORING STORECHECK INSTRUCTION: " + q);
		return false;
	}

	protected boolean processTableSwitch(Quad q) {
		Utilities.debug("IGNORING TABLESWITCH INSTRUCTION: " + q);
		return false;
	}

	protected boolean processUnary(Quad q) {
		Utilities.debug("IGNORING UNARY INSTRUCTION: " + q);
		return false;
	}

	protected boolean processZeroCheck(Quad q) {
		Utilities.debug("IGNORING ZEROCHECK INSTRUCTION: " + q);
		return false;
	}
	
	/**
	 * Inserts in the Quad queue all Quads which depend on {@code q} according
	 * to USE-DEF analysis (i.e., Quads which are using variables defined by
	 * {@code q}). Currently, all quads of a method are inserted in the queue,
	 * but this is clearly sub-optimal.
	 * 
	 * @param q The Quad to be processed.
	 */
	protected void wakeUp(Quad q) {}
	    
	public void printOutput() {}
	
}
