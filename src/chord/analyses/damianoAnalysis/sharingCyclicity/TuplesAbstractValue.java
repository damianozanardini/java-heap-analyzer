package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.RelView;
import chord.bddbddb.Rel.TrioIterable;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Primitive;
import joeq.Compiler.BytecodeAnalysis.BasicBlock;
import joeq.Compiler.Quad.EntryOrExitBasicBlock;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;


/**
 * This class stores and manipulates the abstract information in the tuples implementation.
 * 
 * @author damiano
 *
 */
public class TuplesAbstractValue extends AbstractValue {

	/**
	 * The sharing information
	 */
	private SharingTuples sComp;

	/**
	 * The cyclicity information
	 */
	private CyclicityTuples cComp;
	
	/**
	 * The definite aliasing information
	 */
	private DefiniteAliasingTuples aComp;
	
	/**
	 * The purity information
	 */
	private PurityTuples pComp;
		
	/**
	 * Default constructor.  Create an object with empty abstract information.
	 */
	public TuplesAbstractValue() {
		sComp = new SharingTuples();
		cComp = new CyclicityTuples();
		aComp = new DefiniteAliasingTuples();
		pComp = new PurityTuples();
	}
	
	/**
	 * Creates an object and stores in it the given abstract information.
	 * 	
	 * @param st
	 * @param ct
	 */
	protected TuplesAbstractValue(SharingTuples st,CyclicityTuples ct,DefiniteAliasingTuples at,PurityTuples pt) {
		sComp = st;
		cComp = ct;
		aComp = at;
		pComp = pt;
	}
	
	/**
	 * Update "this" with new abstract information in form of another abstract value.
	 * The "instanceof" test (instead of simply requiring a parameter of type TuplesAbstractValue)
	 * is used because this method overrides the corresponding method in superclasses.
	 * Honestly, we are not sure it is a good design choice.
	 */
	public boolean update(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof TuplesAbstractValue) {
			boolean b = sComp.join(((TuplesAbstractValue) other).getSComp());
			b |= cComp.join(((TuplesAbstractValue) other).getCComp());
			b |= aComp.meet(((TuplesAbstractValue) other).getAComp());
			b |= pComp.join(((TuplesAbstractValue) other).getPComp());
			return b;
		} else // should never happen
			return false;
	}
	
	/**
	 * Returns the sharing component of the abstract value
	 * 
	 * @return
	 */	
	protected SharingTuples getSComp() {
		return sComp;
	}
	
	/**
	 * Returns the cyclicity component of the abstract value
	 * 
	 * @return
	 */
	protected CyclicityTuples getCComp() {
		return cComp;
	}
	
	/**
	 * Returns the definite aliasing component of the abstract value
	 * 
	 * @return
	 */
	protected DefiniteAliasingTuples getAComp() {
		return aComp;
	}
	
	/**
	 * Returns the purity component of the abstract value
	 * 
	 * @return
	 */
	protected PurityTuples getPComp() {
		return pComp;
	}

	/**
	 * Sets the sharing component to the given value (not an update: original information is lost).
	 * 
	 * @param stuples
	 */
	protected void setSComp(SharingTuples stuples){
		this.sComp = stuples;
	}
	
	/**
	 * Sets the cyclicity component to the given value (not an update: original information is lost).
	 * 
	 * @param stuples
	 */
	protected void setCComp(CyclicityTuples ctuples){
		this.cComp = ctuples;
	}

	/**
	 * Sets the definite aliasing component to the given value (not an update: original information is lost).
	 * 
	 * @param stuples
	 */
	protected void setAComp(DefiniteAliasingTuples atuples){
		this.aComp = atuples;
	}

	/**
	 * Sets the purity component to the given value (not an update: original information is lost).
	 * 
	 * @param stuples
	 */
	protected void setPComp(PurityTuples ptuples){
		this.pComp = ptuples;
	}

	/**
	 * Returns a new AbstractValue object with the same abstract information.
	 * The copy is neither completely deep nor completely shallow: each tuple in both
	 * components is duplicated but Register and FieldSet objects are not (they are globally unique).
	 * 
	 * @return a copy of itself
	 */
	public TuplesAbstractValue clone() {
		return new TuplesAbstractValue(sComp.clone(),cComp.clone(),aComp.clone(),pComp.clone());
	}
		
	/**
	 * Adds a tuple to the sharing information.
	 */
	public void addSinfo(Register r1,Register r2,FieldSet fs1,FieldSet fs2) {
		sComp.addTuple(r1,r2,fs1,fs2);
	}
	
	/**
	 * Adds a tuple to the cyclicity information.
	 */
	public void addCinfo(Register r,FieldSet fs) {
		cComp.addTuple(r,fs);
	}
	
	/**
	 * Adds a tuple to the definite aliasing information.
	 */
	public void addAinfo(Register r1,Register r2) {
		aComp.addTuple(r1,r2);
	}

	/**
	 * Adds a tuple to the purity information.
	 */
	public void addPinfo(Register r) {
		pComp.addTuple(r);
	}

	/**
     * Copies information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void copyInfo(Register source,Register dest) {
    		sComp.copyTuples(source,dest);
    		cComp.copyTuples(source,dest);
    		aComp.copyTuples(source,dest);
    		pComp.copyTuples(source,dest);
    }
    
    /**
     * Copies sharing information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void copySinfo(Register source,Register dest) {
    		sComp.copyTuples(source,dest);
    }
    
    /**
     * Copies cyclicity information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void copyCinfo(Register source,Register dest) {
    		cComp.copyTuples(source,dest);
    }
    
    /**
     * Copies definite aliasing information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void copyAinfo(Register source,Register dest) {
    		aComp.copyTuples(source,dest);
    }

    /**
     * Copies purity information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void copyPinfo(Register source,Register dest) {
    		pComp.copyTuples(source,dest);
    }
    
    /**
     * Moves information (both sharing and cyclicity) from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void moveInfo(Register source,Register dest) {
    		sComp.moveTuples(source,dest);
    		cComp.moveTuples(source,dest);
    		aComp.moveTuples(source,dest);
    		pComp.moveTuples(source,dest);
    }
	
    /**
     * Moves sharing information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void moveSinfo(Register source,Register dest) {
    		sComp.moveTuples(source,dest);
    }

    /**
     * Moves cyclicity information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void moveCinfo(Register source,Register dest) {
    		cComp.moveTuples(source,dest);
    }

    /**
     * Moves definite aliasing information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void moveAinfo(Register source,Register dest) {
    		aComp.moveTuples(source,dest);
    }

    /**
     * Moves purity information from a register to another register.
     * 
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void movePinfo(Register source,Register dest) {
    		pComp.moveTuples(source,dest);
    }

    /**
     * Copies the cyclicity information about a register into self-sharing information
     * about the same register.  I.e., for each cyclicity tuple (r,fs), a tuple (r,r,fs,fs) is added
     * to the sharing information.
     */
    protected void copyFromCycle(Register source,Register dest) {
    		sComp.copyTuplesFromCycle(source,dest,cComp);
    }

    /**
     * Removes all the information about a given register.
     */
    public void removeInfo(Register r) {
    		sComp.remove(r);
    		cComp.remove(r);
    		aComp.remove(r);
    		pComp.remove(r);
    }

    public void filterActual(Entry entry,List<Register> actualParameters) {
    		Utilities.begin("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");
		sComp.filterActual(actualParameters);
		cComp.filterActual(actualParameters);
		aComp.filterActual(actualParameters);
		pComp.filterActual(actualParameters);
		Utilities.info("NEW AV: " + this);
		Utilities.end("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");		
	}
    
	/**
	 * Retrieves all tuples (r1,r2,_,_) from sharing information.
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	private ArrayList<Pair<FieldSet,FieldSet>> getSinfo(Register r1,Register r2) {
		return sComp.findTuplesByBothRegisters(r1, r2);
	}

	/**
	 * Retrieves all tuples (r,_) from cyclicity information.
	 * @param r
	 * @return
	 */
	private ArrayList<FieldSet> getCinfo(Register r) {
		return cComp.findTuplesByRegister(r);
	}
	
	/**
	 * Returns true iff both sharing and cyclicity information is empty (no tuples). 
	 * 
	 * @return
	 */
	// WARNING: what about aliasing and purity?
	public boolean isBottom() {
		return sComp.isBottom() && cComp.isBottom();
	}

	/**
	 * Produces a new TuplesAbtsractValue object representing the abstract information after a getfield Quad q,
	 * given "this" as the initial information 
	 */
	public TuplesAbstractValue doGetfield(Entry entry, joeq.Compiler.Quad.Quad q, Register base,
			Register dest, jq_Field field) {
		// verifying if base.field can be non-null; if not, then no new information is
		// produced because dest is certainly null (so, "this" is simply cloned)
		// WARNING PAPER: this is an optimization; add this to the paper?
		List<Pair<FieldSet,FieldSet>> selfSH = getSinfo(base,base);
		boolean shareOnField = false;
		for (Pair<FieldSet,FieldSet> p : selfSH)
			shareOnField |= (p.val0 == FieldSet.addField(FieldSet.emptyset(),field) && p.val1 == p.val0);    			
		if (!shareOnField) {
			Utilities.info("v.f==null: NO NEW INFO PRODUCED");
			return clone();
		}
		// I'_s
		TuplesAbstractValue avIp = clone();
		// copy cyclicity from base to dest, as it is (PAPER: not in JLAMP paper)
		avIp.copyCinfo(base,dest);
		// copy cyclicity of base into self-"reachability" of dest (PAPER: not in JLAMP paper)
		avIp.copyFromCycle(base,dest);
		// copy self-sharing of base into self-sharing of dest, also removing the field
		for (Pair<FieldSet,FieldSet> p : getSinfo(base,base)) {
			FieldSet fs0 = FieldSet.removeField(p.val0,field);
			FieldSet fs1 = FieldSet.removeField(p.val1,field);
			avIp.addSinfo(dest,dest,p.val0,p.val1);
			if (!(p.val0 == p.val1 && p.val0 == FieldSet.addField(FieldSet.emptyset(),field) && !hasNonTrivialCycles(base))) {
				avIp.addSinfo(dest,dest,fs0,p.val1);
				avIp.addSinfo(dest,dest,p.val0,fs1);
			}
			avIp.addSinfo(dest,dest,fs0,fs1);
    		}
		int m = entry.getNumberOfReferenceRegisters();
		for (int i=0; i<m; i++) {
			Register w = entry.getNthReferenceRegister(i);
			// WARNING PAPER: the second conjunct was not in the paper: this is a
			// matter of optimization, even if information is still lost when
			// the ghost copy of base or registers possibly aliasing with base are
			// considered (the latter is required by soundness).
			// A solution would be to implement a DEFINITE ALIASING analysis
			if (w != dest && w != base) {
				for (Pair<FieldSet,FieldSet> p : getSinfo(base,w)) {
					// according to the definition of the \ominus operator
					FieldSet fsl1 = FieldSet.removeField(p.val0,field);
					avIp.addSinfo(dest,w,p.val0,p.val1);
					avIp.addSinfo(dest,w,fsl1,p.val1);
					// according to the definition of the \oplus operator
					if (p.val0 == FieldSet.emptyset()) { 
						FieldSet fsr = FieldSet.addField(p.val1,field);
						avIp.addSinfo(dest,w,p.val0,fsr);
						avIp.addSinfo(dest,w,fsl1,fsr);    				
					}
				}
			}
		}
		return avIp;
	}
	
	/**
	 * Returns true iff a register can be non-trivially cyclic, i.e., iff there
	 * are cycles whose length is greater than 0
	 * 
	 * @param r The register
	 * @return The existence of non-trivial cycles on r
	 */
	private boolean hasNonTrivialCycles(Register r) {
		ArrayList<FieldSet> cycles = getCinfo(r);
		boolean b = false;
		for (FieldSet fs : cycles) b |= fs!=FieldSet.emptyset();
		return b;
	}

	/**
	 * Produces a new TuplesAbtsractValue object representing the abstract information after a putfield Quad q,
	 * given "this" as the initial information.
	 * The computation is taken from both TOCL (the cyclicity part) and JLAMP (the sharing part, sometimes
	 * limited to reachability)
	 */
	public TuplesAbstractValue doPutfield(Entry entry, joeq.Compiler.Quad.Quad q, Register v,
			Register rho, jq_Field field) {
		TuplesAbstractValue avIp = clone();
		int m = entry.getNumberOfReferenceRegisters();
		// I''_s
		TuplesAbstractValue avIpp = new TuplesAbstractValue();
		
		FieldSet z1 = FieldSet.addField(FieldSet.emptyset(),field);
		// computing Z
		List<Pair<FieldSet,FieldSet>> mdls_rhov = avIp.getSinfo(rho,v);
		ArrayList<FieldSet> z2 = new ArrayList<FieldSet>();
		for (Pair<FieldSet,FieldSet> p : mdls_rhov)
			if (p.val1 == FieldSet.emptyset()) z2.add(p.val0);
		// main loop
		for (int i=0; i<m; i++) {
			for (int j=0; j<m; j++) {
				Register w1 = entry.getNthReferenceRegister(i);
				Register w2 = entry.getNthReferenceRegister(j);
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
		// cyclicity
		// PAPER: there seems to be an unsoundness issue in TOCL14: cyclicitly should also be updated if 
		// I'_r(w,rho) \neq false (not only if I'_r(w,v) \new false); however, the former implies the latter
		// because, if w reaches rho, and some new cycle is created, then rho has to reach v, so that, in the end,
		// w also reach v.
		for (int i=0; i<m; i++) {
			Register w = entry.getNthReferenceRegister(i);
			boolean reaches = false;
			for (Pair<FieldSet,FieldSet> w_to_v : avIp.getSinfo(w,v))
				reaches |=  (w_to_v.val1 == FieldSet.emptyset() || w_to_v.val1 == FieldSet.addField(FieldSet.emptyset(),field));
			if (reaches) {
				avIpp.copyCinfo(rho,w);
				for (Pair<FieldSet,FieldSet> rho_to_v : avIp.getSinfo(rho,v)) {
					if (rho_to_v.val1 == FieldSet.emptyset()) { // reachability from rho to v
						FieldSet fs = FieldSet.addField(rho_to_v.val0,field);
						avIpp.addCinfo(w,fs);						
					}
				}
			}
		}
		avIp.update(avIpp);
		return avIp;
	}

	/**
	 * 
	 */
	public TuplesAbstractValue doInvoke(Entry entry, Entry invokedEntry,
			joeq.Compiler.Quad.Quad q, ArrayList<Register> actualParameters,Register rho) {
		// copy of I_s
    		TuplesAbstractValue avIp = clone();
    		// only actual parameters are kept; av_Ip becomes I'_s in the paper
    		avIp.filterActual(entry,actualParameters);
    		Utilities.info("I'_s = " + avIp);
    	
    		// this produces I'_s[\bar{v}/mth^i] in the paper, where the abstract information
    		// is limited to the formal parameters of the invoked entry
    		avIp.actualToFormal(actualParameters,invokedEntry);
    		// I'_s[\bar{v}/mth^i] is used to update the input of the summary of the invoked entry;
    		// if the new information is not included in the old one, then the invoked entry needs
    		// to be re-analyzed
    		if (GlobalInfo.summaryManager.updateSummaryInput(invokedEntry,avIp)) GlobalInfo.wakeUp(invokedEntry);
    		// this is \absinterp(mth)(I'_s[\bar{v}/mth^i]), although, of course,
    		// the input and output components of a summary are not "synchronized"
    		// (we've just produced a "new" input, but we are still using the "old"
    		// output while waiting for the entry to be re-analyzed)
    		TuplesAbstractValue avIpp = null;
    		if (GlobalInfo.bothImplementations())
    			avIpp = ((BothAbstractValue) GlobalInfo.summaryManager.getSummaryOutput(invokedEntry)).getTuplesPart().clone();
    		else avIpp = ((TuplesAbstractValue) GlobalInfo.summaryManager.getSummaryOutput(invokedEntry)).clone();
    		// this generates I''_s, which could be empty if no summary output is available
    		// WARNING: have to take the return value into account
    		if (avIpp != null) {
    			avIpp.cleanGhostRegisters(invokedEntry);
    			avIpp.formalToActual(actualParameters,rho,invokedEntry);
    		} else avIpp = new TuplesAbstractValue();
    		Utilities.info("I''_s = " + avIpp);
    		
    		// start computing I'''_s
    		Utilities.begin("COMPUTING I'''_s");
    		TuplesAbstractValue avIppp = new TuplesAbstractValue();
    		int m = entry.getNumberOfReferenceRegisters();
    		int n = actualParameters.size();
    		TuplesAbstractValue[][] avs = new TuplesAbstractValue[n][n];
    		// computing each I^{ij}_s
    		for (int i=0; i<n; i++) {
    			for (int j=0; j<n; j++) {
    				avs[i][j] = new TuplesAbstractValue();
    				// WARNING: can possibly filter out non-reference registers
    				Register vi = actualParameters.get(i);
    				Register vj = actualParameters.get(j);
    				for (int k1=0; k1<m; k1++) { // for each w_1 
    					for (int k2=0; k2<m; k2++) { // for each w_2
    						Register w1 = entry.getNthReferenceRegister(k1);
    						Register w2 = entry.getNthReferenceRegister(k2);
    						for (Pair<FieldSet,FieldSet> pair_1 : getSinfo(w1,vi)) { // \omega_1(toRight)
    							for (Pair<FieldSet,FieldSet> pair_2 : getSinfo(vj,w2)) { // \omega_2(toLeft)
    								for (Pair<FieldSet,FieldSet> pair_ij : avIpp.getSinfo(vi,vj)) { // \omega_ij
    									// Utilities.info("FOUND: vi = " + vi + ", vj = " + vj + ", w1 = " + w1 + ", w2 = " + w2 + ", pair_1 = " + pair_1 + ", pair_2 = " + pair_2 + ", pair_ij = " + pair_ij);
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
    		// computing I''''_s
    		Utilities.begin("COMPUTING I''''_s");
    		TuplesAbstractValue avIpppp = new TuplesAbstractValue();
    		if (rho != null) {
    			Utilities.info("METHOD WITH RETURN VALUE " + rho);
    			for (int i=0; i<m; i++) {
    				Register w = entry.getNthReferenceRegister(i);
    				for (int k=0; k<n; k++) {
    					// computing each F_i
    					Register vk = actualParameters.get(k);
    					for (Pair<FieldSet,FieldSet> omega0 : getSinfo(vk,w))
    						for (Pair<FieldSet,FieldSet> omega1 : avIpp.getSinfo(vk,rho))
    							for (Pair<FieldSet,FieldSet> omega2 : avIpp.getSinfo(rho,vk)) {
    								Utilities.info("omega0 = " + omega0 + "; omega1 = " + omega1 + "; omega2 = " + omega2);
    								for (FieldSet x : omega1.val0.getSubsets()) {
    									FieldSet fs = omega0.val0;
    									fs = FieldSet.setDifference(fs,x);
    									fs = FieldSet.union(fs,omega2.val0);
    									avIpppp.addSinfo(rho,w,fs,omega0.val1);
    								}
    							}
    				}
    			}
    		} else Utilities.info("METHOD WITH NO RETURN VALUE");
    		Utilities.end("COMPUTING I''''_s");

    		// computing the final union I_s \vee I'_s \vee I''_s \vee I'''_s \vee I''''_s
    		// WARNING: I''''_s is still not here
    		TuplesAbstractValue avOut = clone();
    		avOut.removeInfoList(actualParameters);
    		avOut.update(avIpp);
    		avOut.update(avIppp);
    		avOut.update(avIpppp);
    		Utilities.info("FINAL UNION: " + avOut);
    		return avOut;
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

	public ArrayList<Pair<FieldSet, FieldSet>> getStuples(Register r1,
			Register r2) {
		return getSinfo(r1,r2);
	}

	public ArrayList<FieldSet> getCtuples(Register r) {
		return getCinfo(r);
	}

	public boolean equals(AbstractValue av) {
		if (av instanceof TuplesAbstractValue)
			return sComp.equals(((TuplesAbstractValue) av).getSComp()) &&
					cComp.equals(((TuplesAbstractValue) av).getCComp()) &&
					aComp.equals(((TuplesAbstractValue) av).getAComp()) &&
					pComp.equals(((TuplesAbstractValue) av).getPComp());
		else return false;
	}
	
	public void sort() {
		sComp.sort();
		cComp.sort();
		aComp.sort();
		pComp.sort();
	}
	
	/**
	 * The usual toString method.
	 */
	public String toString() {
		return sComp.toString() + " / " + cComp.toString() + " / "
				+ aComp.toString() + " / " + pComp.toString();
	}

}
