package chord.analyses.damianoAbstractSlicing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
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
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Ret;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Special;
import joeq.Compiler.Quad.Operator.StoreCheck;
import joeq.Compiler.Quad.Operator.TableSwitch;
import joeq.Compiler.Quad.Operator.Unary;
import joeq.Compiler.Quad.Operator.ZeroCheck;
import joeq.Compiler.Quad.PrintCFG;
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
 * Implementation of the fixpoint.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
public class ASlicingFixpoint extends Fixpoint {
	/**
	 * Program-point-wise information about agreements
	 */
	private AgreementList agreementList;
		
	/**
	 * Reads lines from file {@code <Config.workDirName>/input};
	 */

	public void parseInputLine(String line0)  throws ParseInputLineException {
		Utilities.debug("  PARSING LINE: '" + line0 + "'");
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return; // empty line
		Agreement finalAgreement = new Agreement();
		String[] tokens = line.split(" ");
		if (tokens[0].equals("M")) {
			setMethod(tokens[1]);
			return;
		}
		if (tokens[0].equals("aslicing")) {
			if (tokens[1].equals("?")) {
				Register r = null;
				for (int i=2; i<tokens.length; i++) {
					r = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[i]);
					finalAgreement.put(r,new Nullity(Nullity.NULL));
				}
				Utilities.debug0("  LINE '" + line0 + "' PARSED TO ---> ");
				finalAgreement.showMe();
			} else {
				Utilities.debug("  NOT AN INPUT LINE: '" + line0 + "'");
			}
			if (finalAgreement != null) { // there is actually an input
				Quad q = getFinalQuad();
				agreementList.update(q,finalAgreement);			
			}
		}
	}
	
	/**
	 * This method sets the analysis information and prepares the analysis.
	 */
	public void init() {
		Utilities.debug("*** ===============================================================");
		Utilities.debug("*** BEGIN INITIALIZATION");
		agreementList = new AgreementList();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(Config.workDirName + "/input"));
			readInputFile(br);
		} catch (FileNotFoundException e) {
			Utilities.out("ERROR: file " + Config.workDirName + "/input" + " not found");
			readInputFile();
		}
				
		// debug-only
		ControlFlowGraph cfg = CodeCache.getCode(getMethod());
	    new PrintCFG().visitCFG(cfg);
		
		// setting input (slicing criterion)
		try {
			BufferedReader br = new BufferedReader(new FileReader(Config.workDirName + "/input")); // back to the beginning
			readInputFile(br);
		} catch (FileNotFoundException e) {
			Utilities.out("ERROR: file " + Config.workDirName + "/input" + " not found");
			readInputFile();
		}
		
		// outputting source-code variables corresponding to registers
		RegisterManager.printVarRegMap(getMethod());
		
		// initializing the queue
        jq_Method meth = getMethod();
        Utilities.out("ANALYSIS METHOD: " + meth);
    	queue = new QuadQueue(meth,QuadQueue.BACKWARD);
    	
    	agreementList.showMe();
		Utilities.debug("*** END INITIALIZATION");
	}
	
	public Pair<jq_Method,AgreementList> getAgreementList() {
		run();
		return new Pair<jq_Method,AgreementList>(getMethod(),agreementList);
	}
	
	private Quad getFinalQuad() {
		ControlFlowGraph cfg = CodeCache.getCode(getMethod());
	    // TODO the assumption here is that the exit block has only one predecessor
	    BasicBlock semiLastBlock = cfg.exit().getPredecessors().get(0);
	    List<Quad> quadList = semiLastBlock.getQuads();
	    Quad q =  (quadList.get(quadList.size()-1));
	    Utilities.debug("LAST QUAD IS " + q);
	    return q;
	}

	protected boolean process(Quad q) {
		boolean x = super.process(q);
		agreementList.showMe();
		return x;
	}
	
	protected boolean processALength(Quad q) {
		Utilities.debug("IGNORING ALENGTH INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}
	
	protected boolean copyToPrevQuads(Quad q) {
		List<Quad> pqs = getPrevQuads(q);
		return copyAgreement(q,pqs);
	}

    private boolean copyAgreement(Quad src,List<Quad> dests) {
		Agreement asrc = agreementList.get(src);
		Utilities.debug("  COPYING AGREEMENT " + asrc.toString() + " AT " + src + " TO " + dests + "...");
		boolean x = false;
		for (Quad dest : dests) {
			x |= agreementList.update(dest,asrc);
		}
		Utilities.debug(" DONE");
		return x;
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
    	List<Quad> pqs = getPrevQuads(q);
    	return copyAgreementSrcDest(q,base,pqs,dest);
    }

    private List<Quad> getPrevQuads(Quad q) {
    	BasicBlock bb = q.getBasicBlock();
    	List<Quad> qpreds = new ArrayList<Quad>();
    	int n = bb.getQuadIndex(q);
    	if (n == 0 || (q.getOperator() instanceof Phi)) { // first quad in the basic block, or a PHI quad	    	
    		List<BasicBlock> bbpreds = bb.getPredecessors();
    		for (BasicBlock bb0 : bbpreds) {
    			if (!bb0.isEntry()) qpreds.add(bb0.getLastQuad());
    		}
    		Utilities.debug("  QUADS PREVIOUS TO " + q + " = " + qpreds.toString());
    		return qpreds;
    	} else {
    		qpreds.add(bb.getQuad(n-1));
    		Utilities.debug("  QUADS PREVIOUS TO " + q + " = " + qpreds.toString());
    		return qpreds;
    	}
	}
    
    /**
     * This method retrieves the abstract value corresponding to dest at q, and
     * joins it to the one corresponding to base at pq.
     * 
     * @param pq
     * @param q
     * @param dest
     * @param base
     * @return
     */
    private boolean copyAgreementSrcDest(Quad qsrc,Register rsrc,List<Quad> qdests,Register rdest) {
    	Agreement asrc = agreementList.get(qsrc);
    	if (asrc != null) {
    		AbstractValue avsrc = asrc.get(rsrc);
    		boolean x = false;
    		for (Quad qdest : qdests) {
    			Agreement adest = agreementList.get(qdest);
    			if (adest != null) {
    				adest.lub(rdest,avsrc);
    				x |= agreementList.update(qdest,adest);
    			} else {
    				adest = asrc.clone();
    				Utilities.debug("  CLONED AGREEMENT: " + asrc.toString() + " ---> " + adest.toString());
    				adest.remove(rsrc);
    				adest.lub(rdest,avsrc);
    				x |= agreementList.update(qdest,adest);
    			}
    			Utilities.debug("  AGREEMENT " + asrc.toString() + " (" + rsrc + ") COPIED TO QUAD " + qdest + " (" + rdest + "), RESULTING IN " + adest.toString());
    		}
    		return x;
    	} else return false;
	}

	/**
     * This method simply copies the information about the value into the array
     * variable, unless the value has primitive type.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processAStore(Quad q) {
    	if (((RegisterOperand) AStore.getValue(q)).getType().isPrimitiveType()) {
    		Utilities.debug("PROCESSING ASTORE INSTRUCTION ON PRIMTIVE TYPE: " + q);
    		return copyToPrevQuads(q);
    	}
    	Utilities.debug("PROCESSING ASTORE INSTRUCTION: " + q);
    	Register base = ((RegisterOperand) AStore.getBase(q)).getRegister();
    	Register value = ((RegisterOperand) AStore.getValue(q)).getRegister();
    	List<Quad> pqs = getPrevQuads(q);
    	return copyAgreementSrcDest(q,base,pqs,value);
    }
    
    protected boolean processBinary(Quad q) {
		Utilities.debug("IGNORING BINARY INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}
    
	protected boolean processBoundsCheck(Quad q) {
		Utilities.debug("IGNORING BOUNDSCHECK INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}

    protected boolean processBranch(Quad q) {
		Utilities.debug("IGNORING BRANCH INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}

	protected boolean processCheckCast(Quad q) {
		Utilities.debug("IGNORING CHECKCAST INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}
	
	protected boolean processGetfield(Quad q) {
		Utilities.debug("PROCESSING GETFIELD INSTRUCTION: " + q);
		boolean x = false;
		List<Quad> pqs = getPrevQuads(q);
		Agreement a = agreementList.get(q);
		Register base = ((RegisterOperand) Getfield.getBase(q)).getRegister();
    	Register dest = ((RegisterOperand) Getfield.getDest(q)).getRegister();
		if (a != null) {
			for (Quad pq : pqs) {
				Agreement aa = a.clone();
				AbstractValue avdest = aa.get(dest);
				aa.remove(dest);
				// the interest on base before the getfield is ID unless there
				// was no interest on dest after the getfield
				// TODO this is only meaningful to nullity
				if (avdest != null) {
					if (!avdest.isTop()) aa.put(base,avdest.buildId());
				}		
				Agreement qdest = agreementList.get(pq);
				if (qdest != null) {
					x |= qdest.lub(aa);
				} else {
					x |= agreementList.update(pq,aa);
				}
			}
		}
		return x;
	}
		
	// this case should be, and actually is, already included in BRANCH, so
	// that processGoto is never invoked
	protected boolean processGoto(Quad q) {
		Utilities.debug("IGNORING GOTO INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}
	
	protected boolean processInstanceOf(Quad q) {
		Utilities.debug("IGNORING INSTANCEOF INSTRUCTION: " + q);
		return copyToPrevQuads(q);
    }
	
    protected boolean processIntIfCmp(Quad q) {
		Utilities.debug("IGNORING INTIFCMP INSTRUCTION: " + q);
		return copyToPrevQuads(q);
    }
    
	protected boolean processInvoke(Quad q) {	
		// TODO interprocedurality currently unsupported
		Operator operator = q.getOperator();
		if ((operator instanceof InvokeStatic) &&
				q.getOp2().toString().matches("(.*)<init>(.*)")) {
			Utilities.debug("PROCESSING <init> INVOKESTATIC: " +q);
		} else   			
			Utilities.debug("IGNORING INVOKE INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}

	protected boolean processJsr(Quad q) {
		Utilities.debug("IGNORING JSR INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}
	
	protected boolean processLookupSwitch(Quad q) {
		Utilities.debug("IGNORING LOOKUPSWITCH INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}

	protected boolean processMemLoad(Quad q) {
		Utilities.debug("IGNORING MEMLOAD INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}

	protected boolean processMemStore(Quad q) {
		Utilities.debug("IGNORING MEMSTORE INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}
	
	protected boolean processMonitor(Quad q) {
		Utilities.debug("IGNORING MONITOR INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}

    protected boolean processMove(Quad q) {
    	if (((RegisterOperand) AStore.getValue(q)).getType().isPrimitiveType()) {
        	Utilities.debug("IGNORING MOVE INSTRUCTION WITH PRIMITIVE TYPE: " + q);
    		return copyToPrevQuads(q);
    	}
    	Utilities.debug("PROCESSING MOVE INSTRUCTION: " + q);
    	Register dest = ((RegisterOperand) Move.getDest(q)).getRegister();
		List<Quad> pqs = getPrevQuads(q);
		Operand opsrc = (Operand) Move.getSrc(q);
		if (opsrc instanceof RegisterOperand) { // from register to register
    		Register src = ((RegisterOperand) opsrc).getRegister();
    		// be careful: the source-and-dest of the MOVE instruction are,
    		// respectively, the dest-and-source of copyAgreementSrcDest
    		return copyAgreementSrcDest(q,dest,pqs,src);
    	} else { // from constant to register
    		return copyAgreementSrcConst(q,dest,pqs);
    	}
    }
    
    private boolean copyAgreementSrcConst(Quad qsrc,Register rsrc,List<Quad> qdests) {
    	boolean x = false;
    	Agreement asrc = agreementList.get(qsrc);
    	if (asrc != null) {
    		for (Quad qdest : qdests) {
    			Agreement adest = agreementList.get(qdest);
    			if (adest != null) {
    				Agreement a = asrc.clone();
    				a.remove(rsrc);
    				x |= adest.lub(a);
    			}
    		}
    	}
    	return x;
    }
    
    protected boolean processNew(Quad q) {
    	Utilities.debug("PROCESSING NEW INSTRUCTION: " + q);
    	List<Quad> pqs = getPrevQuads(q);
    	Agreement aq = agreementList.get(q);
    	Agreement aqcopy;
    	boolean x = false;
    	if (aq == null) {
    		aqcopy = new Agreement();    		
    	} else {
    		aqcopy = aq.clone();
    		aqcopy.remove(New.getDest(q).getRegister());
    	}
    	for (Quad pq : pqs) {
			x |= agreementList.update(pq,aqcopy);
    	}
    	return x;
    }
    
    protected boolean processPhi(Quad q) {
		boolean x = false;
		x |= copyAgreement_phi(q,getPrevQuads(q));		
		x |= copyAgreement(q,getPrevPhi(q));
		return x;
	}
	
    private List<Quad> getPrevPhi(Quad q) {
    	BasicBlock bb = q.getBasicBlock();
    	List<Quad> qpreds = new ArrayList<Quad>();
    	int n = bb.getQuadIndex(q);
    	if (n > 0) {
    		qpreds.add(bb.getQuad(n-1));
    	}
    	Utilities.debug("  PHI PREVIOUS TO " + q + " = " + qpreds.toString());
    	return qpreds;
    }

	private boolean copyAgreement_phi(Quad src,List<Quad> dests) {
		Utilities.debug("  COPYING PHI-AGREEMENT AT " + src + " TO " + dests + "...");
		boolean x = false;
		if (src.getOperator() instanceof Phi) { // should be always the case
			Register r1 = ((RegisterOperand) Phi.getSrc(src,0)).getRegister();
	    	Register r2 = ((RegisterOperand) Phi.getSrc(src,1)).getRegister();
	    	Register r = ((RegisterOperand) Phi.getDest(src)).getRegister();
	    	x |= copyAgreementSrcDest(src,r,dests.subList(0,1),r1);
	    	x |= copyAgreementSrcDest(src,r,dests.subList(1,2),r2);
	    	Utilities.debug(" DONE");
			return x;
		} else { // should never be the case
			return false;
		}
	}
    
    protected boolean processPutfield(Quad q) {
		Utilities.debug("PROCESSING PUTFIELD INSTRUCTION: " + q);
		return copyToPrevQuads(q);
	}

    protected boolean processReturn(Quad q) {
		Utilities.debug("IGNORING RETURN INSTRUCTION: " + q);
		System.out.println("AGREEMENT:::::::: " + agreementList.get(q).toString());
		return copyToPrevQuads(q);
	}
    
    protected void wakeUp(Quad q) {
		queue.fill_bw(getMethod());
	}
    
}
