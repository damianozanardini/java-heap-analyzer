package chord.analyses.damianoAnalysis.abstractSlicing;

import java.util.ArrayList;
import java.util.List;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
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
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
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
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.util.tuple.object.Pair;

public class Slicer {

	List<Pair<Quad,Boolean>> markings;
	
	jq_Method method;
	
	public Slicer(jq_Method m) {
		method = m;
		markings = new ArrayList<Pair<Quad,Boolean>>();
		ControlFlowGraph cfg = CodeCache.getCode(method);
		List<BasicBlock> bbs = cfg.reversePostOrderOnReverseGraph();
	    for (BasicBlock bb : bbs) {
	    	for (Quad qq : bb.getQuads()) {
	    		markings.add(new Pair<Quad,Boolean>(qq,true));
	    	}
	    }
	}
	
	public void run(AgreementList alist) {
		Utilities.out("*** ===============================================================");
		Utilities.out("*** BEGIN SLICING");
		ControlFlowGraph cfg = CodeCache.getCode(method);
		List<BasicBlock> bbs = cfg.reversePostOrderOnReverseGraph();
	    for (BasicBlock bb : bbs) {
	    	for (Quad qq : bb.getQuads()) {
	    		Utilities.debug("  ANALYZING QUAD " + qq);
	    		Agreement a = alist.get(qq);
	    		if (a != null) {
	    			Utilities.debug("    AGREEMENT FOR " + qq + ": " + a.toString());
	    			if (isInvariant(qq,a)) {
	    				Utilities.out("    QUAD " + qq + " MARKED AS IRRELEVANT");
	    				setMarkings(qq,false);
	    			}
	    		} else {
	    			Utilities.debug("    AGREEMENT FOR " + qq + ": NOT FOUND");
	    		}
	    	}
	    }
	    printResults();
	    Utilities.out("*** END SLICING");
	}

	private boolean isInvariant(Quad q, Agreement a) {
		Signature signature = getSignature(q);
		Utilities.debug("    SIGNATURE FOR " + q + ": " + signature);
		return signature.isInvariant(a);
	}

	private Signature getSignature(Quad q) {
		Signature signature = new Signature();
		Operator operator = q.getOperator();
		if (operator instanceof ALength) {
		} else if (operator instanceof ALoad) {
			Register dest = ((RegisterOperand) ALoad.getDest(q)).getRegister();
			signature.put(dest,new Nullity(Nullity.TOP));
		} else if (operator instanceof AStore) {
			Register base = ((RegisterOperand) AStore.getBase(q)).getRegister();
			signature.put(base,new Nullity(Nullity.TOP));
		} else if (operator instanceof Binary) {	
		} else if (operator instanceof BoundsCheck) {
		} else if (operator instanceof Branch) {
			// extended by Goto, IntIfCmp, Jsr, LookupSwitch, Ret, TableSwitch
			DomV domV = (DomV) ClassicProject.g().getTrgt("V");
			for (int i=0; i<domV.size(); i++) {
				Register r = domV.get(i);
				signature.put(r,new Nullity(Nullity.TOP));
			}
		} else if (operator instanceof CheckCast) {
		} else if (operator instanceof Getfield) {
			Register dest = ((RegisterOperand) Getfield.getDest(q)).getRegister();
			signature.put(dest,new Nullity(Nullity.TOP));
		} else if (operator instanceof Getstatic) {
		} else if (operator instanceof InstanceOf) {
		} else if (operator instanceof Invoke) {
		} else if (operator instanceof MemLoad) {
		} else if (operator instanceof MemStore) {
		} else if (operator instanceof Monitor) {
		} else if (operator instanceof Move) {
			Register dest = ((RegisterOperand) Move.getDest(q)).getRegister();
			signature.put(dest,new Nullity(Nullity.TOP));
		} else if (operator instanceof MultiNewArray) {
			Register dest = ((RegisterOperand) MultiNewArray.getDest(q)).getRegister();
			signature.put(dest,new Nullity(Nullity.NULL));
		} else if (operator instanceof New) {
			Register dest = ((RegisterOperand) New.getDest(q)).getRegister();
			signature.put(dest,new Nullity(Nullity.NULL));
		} else if (operator instanceof NewArray) { 
			Register dest = ((RegisterOperand) NewArray.getDest(q)).getRegister();
			signature.put(dest,new Nullity(Nullity.NULL));
		} else if (operator instanceof NullCheck) {
		} else if (operator instanceof Phi) {
			Register dest = ((RegisterOperand) Phi.getDest(q)).getRegister();
			signature.put(dest,new Nullity(Nullity.TOP));
		} else if (operator instanceof Putfield) {
			Register base = ((RegisterOperand) Putfield.getBase(q)).getRegister();
			signature.put(base,new Nullity(Nullity.NULL));
		} else if (operator instanceof Putstatic) {
		} else if (operator instanceof Return) {
			// TODO: this seems incorrect because src is not modified by the instruction;
			// however, it is the only way not to have all return sliced away
			// In order to fit in the usual framework, we can think that a
			// return instruction stores its src into the return register, so
			// that it is not sliced away as long as there is interest on the
			// return value
			RegisterOperand ro = ((RegisterOperand) Return.getSrc(q));
			if (ro != null) {
				Register src = ro.getRegister();
				Utilities.debug("  SRC REGISTER: " + src);
				signature.put(src,new Nullity(Nullity.TOP));
				Utilities.debug("  TOP ADDED FOR RETURN QUAD");
			}
		} else if (operator instanceof Special) {
		} else if (operator instanceof StoreCheck) {
		} else if (operator instanceof Unary) {
		} else if (operator instanceof ZeroCheck) { }
		return signature;
	}
	
	private void setMarkings(Quad qq, boolean b) {
		for (int i = 0; i<markings.size(); i++) {
			Pair<Quad,Boolean> p = markings.get(i);
			if (p.val0 == qq) {
				markings.set(i,new Pair<Quad,Boolean>(qq,b));
				return;
			}
		}
	}

	private void printResults() {
		Utilities.out("*** ===============================================================");
		Utilities.out("*** SLICING RESULTS:");
		for (int i = 0; i<markings.size(); i++) {
			Pair<Quad,Boolean> p = markings.get(i);
			int n = p.val0.toString().length();
			if (p.val1) {
				Utilities.out0(p.val0 + " ");
				for	(int j = n; j < 50; j++)
					Utilities.out0("=");
				Utilities.out(" " + p.val0);
			} else Utilities.out(p.val0.toString());
		}		
	}
	
	
	
	
	
}
