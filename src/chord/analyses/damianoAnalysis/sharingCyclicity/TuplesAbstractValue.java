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
 * This class stores and manipulates the abstract information in the tuples
 * implementation.
 * 
 * @author damiano
 *
 */
public class TuplesAbstractValue extends AbstractValue {

	/**
	 * The sharing information.
	 */
	private SharingTuples sComp;

	/**
	 * The cyclicity information.
	 */
	private CyclicityTuples cComp;
	
	/**
	 * The definite aliasing information.
	 */
	private DefiniteAliasingTuples aComp;
	
	/**
	 * The purity information.
	 */
	private PurityTuples pComp;
		
	/**
	 * Default constructor.  It creates an object with empty abstract information.
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
	 * Updates {@code this} with new abstract information in form of another
	 * abstract value.  The {@code instanceof} test (instead of simply requiring
	 * a parameter of type {@code TuplesAbstractValue}) is used because this
	 * method overrides the corresponding method in superclasses.  Honestly, we
	 * are not sure it is a good design choice.
	 */
	public boolean updateSInfo(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof TuplesAbstractValue) {
			return sComp.join(((TuplesAbstractValue) other).getSComp());
		} else // should never happen
			return false;
	}

	/**
	 * Updates {@code this} with new abstract information in form of another
	 * abstract value.  The {@code instanceof} test (instead of simply requiring
	 * a parameter of type {@code TuplesAbstractValue}) is used because this
	 * method overrides the corresponding method in superclasses.  Honestly, we
	 * are not sure it is a good design choice.
	 */
	public boolean updateCInfo(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof TuplesAbstractValue) {
			return cComp.join(((TuplesAbstractValue) other).getCComp());
		} else // should never happen
			return false;
	}

	/**
	 * Updates {@code this} with new abstract information in form of another
	 * abstract value.  The {@code instanceof} test (instead of simply requiring
	 * a parameter of type {@code TuplesAbstractValue}) is used because this
	 * method overrides the corresponding method in superclasses.  Honestly, we
	 * are not sure it is a good design choice.
	 */
	public boolean updateAInfo(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof TuplesAbstractValue) {
			return aComp.meet(((TuplesAbstractValue) other).getAComp());
		} else // should never happen
			return false;
	}

	/**
	 * Updates {@code this} with new abstract information in form of another
	 * abstract value.  The {@code instanceof} test (instead of simply requiring
	 * a parameter of type {@code TuplesAbstractValue}) is used because this
	 * method overrides the corresponding method in superclasses.  Honestly, we
	 * are not sure it is a good design choice.
	 */
	public boolean updatePInfo(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof TuplesAbstractValue) {
			return pComp.join(((TuplesAbstractValue) other).getPComp());
		} else // should never happen
			return false;
	}

	public void clearPInfo() {
		pComp = new PurityTuples();
	}
		
	/**
	 * Returns the sharing component of the abstract value.
	 * 
	 * @return the sharing component
	 */	
	public SharingTuples getSComp() {
		return sComp;
	}
	
	/**
	 * Returns the cyclicity component of the abstract value.
	 * 
	 * @return the cyclicity component
	 */
	public CyclicityTuples getCComp() {
		return cComp;
	}
	
	/**
	 * Returns the definite aliasing component of the abstract value.
	 * 
	 * @return the definite aliasing component
	 */
	public DefiniteAliasingTuples getAComp() {
		return aComp;
	}
	
	/**
	 * Returns the purity component of the abstract value.
	 * 
	 * @return the purity component
	 */
	public PurityTuples getPComp() {
		return pComp;
	}

	/**
	 * Sets the sharing component to the given value (not an update: original
	 * information is lost).
	 * 
	 * @param stuples
	 */
	public void setSComp(SharingTuples stuples){
		this.sComp = stuples;
	}
	
	/**
	 * Sets the cyclicity component to the given value (not an update: original
	 * information is lost).
	 * 
	 * @param stuples
	 */
	public void setCComp(CyclicityTuples ctuples){
		this.cComp = ctuples;
	}

	/**
	 * Sets the definite aliasing component to the given value (not an update:
	 * original information is lost).
	 * 
	 * @param stuples
	 */
	public void setAComp(DefiniteAliasingTuples atuples){
		this.aComp = atuples;
	}

	/**
	 * Sets the purity component to the given value (not an update: original
	 * information is lost).
	 * 
	 * @param stuples
	 */
	public void setPComp(PurityTuples ptuples){
		this.pComp = ptuples;
	}

	public TuplesAbstractValue clone() {
		return new TuplesAbstractValue(sComp.clone(),cComp.clone(),aComp.clone(),pComp.clone());
	}
		
	public void addSInfo(Register r1,Register r2,FieldSet fs1,FieldSet fs2) {
		sComp.addTuple(r1,r2,fs1,fs2);
	}
	
	public void addCInfo(Register r,FieldSet fs) {
		cComp.addTuple(r,fs);
	}
	
	public void addAInfo(Register r1,Register r2) {
		aComp.addTuple(r1,r2);
	}

	public void addPInfo(Register r) {
		pComp.addTuple(r);
	}

    public void copyInfo(Register source,Register dest) {
    		sComp.copyInfo(source,dest);
    		cComp.copyInfo(source,dest);
    		aComp.copyInfo(source,dest);
    		pComp.copyInfo(source,dest);
    }
    
    public void copySInfo(Register source,Register dest) {
    		sComp.copyInfo(source,dest);
    }
    
    public void copyCInfo(Register source,Register dest) {
    		cComp.copyInfo(source,dest);
    }
    
    public void copyAInfo(Register source,Register dest) {
    		aComp.copyInfo(source,dest);
    }

    public void copyPInfo(Register source,Register dest) {
    		pComp.copyInfo(source,dest);
    }
    
    /**
     * Copies sharing information from a register of another abstract value to
     * another register of the current abstract value.
     * 
     * @param other The other abstract value
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void copySinfoFrom(TuplesAbstractValue other,Register source,Register dest) {
    		sComp.copyInfoFrom(other.getSComp(),source,dest);
    }
    
    /**
     * Copies all the sharing information from another abstract value.
     * 
     * @param other The other abstract value
     */
    public void copySinfoFrom(TuplesAbstractValue other) {
		sComp = other.getSComp().clone();
    }
    
    /**
     * Copies cyclicity information from a register of another abstract value to
     * another register of the current abstract value.
     * 
     * @param other The other abstract value
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void copyCinfoFrom(TuplesAbstractValue other,Register source,Register dest) {
    		cComp.copyInfoFrom(other.getCComp(),source,dest);
    }
    
    /**
     * Copies all the cyclicity information from another abstract value.
     * 
     * @param other The other abstract value
     */
    public void copyCinfoFrom(TuplesAbstractValue other) {
		cComp = other.getCComp().clone();
    }

    /**
     * Copies all the definite aliasing information from another abstract value.
     * 
     * @param other The other abstract value
     */
    public void copyAinfoFrom(TuplesAbstractValue other) {
		aComp = other.getAComp().clone();
    }

    /**
     * Copies purity information from a register of another abstract value to
     * another register of the current abstract value.
     * 
     * @param other The other abstract value
     * @param source The source register.
     * @param dest The destination register.
     * @return
     */
    public void copyPinfoFrom(TuplesAbstractValue other,Register source,Register dest) {
    		pComp.copyInfoFrom(other.getPComp(),source,dest);
    }

    /**
     * Copies all the purity information from another abstract value.
     * 
     * @param other The other abstract value
     */
    public void copyPinfoFrom(TuplesAbstractValue other) {
		pComp = other.getPComp().clone();
    }

    public void moveInfo(Register source,Register dest) {
    		sComp.moveInfo(source,dest);
    		cComp.moveInfo(source,dest);
    		aComp.moveInfo(source,dest);
    		pComp.moveInfo(source,dest);
    }
	
    public void moveSInfo(Register source,Register dest) {
    		sComp.moveInfo(source,dest);
    }

    public void moveCInfo(Register source,Register dest) {
    		cComp.moveInfo(source,dest);
    }

    public void moveAInfo(Register source,Register dest) {
    		aComp.moveInfo(source,dest);
    }

    public void movePInfo(Register source,Register dest) {
    		pComp.moveInfo(source,dest);
    }

    public void copySInfoFromC(Register source,Register dest) {
    		sComp.copyTuplesFromCycle(source,dest,cComp);
    }
    
    public void removeInfo(Register r) {
    		removeSInfo(r);
    		removeCInfo(r);
    		removeAInfo(r);
    		removePInfo(r);
    }

    public void removeSInfo(Register r) {
    		sComp.remove(r);
    }

    public void removeCInfo(Register r) {
    		cComp.remove(r);
    }

    public void removeAInfo(Register r) {
    		aComp.remove(r);
    }

    public void removePInfo(Register r) {
    		pComp.remove(r);
    }

    private void clearPurity() {
    		pComp.clear();
    }

    public void filterActual(Entry entry,List<Register> actualParameters) {
    		Utilities.begin("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");
		sComp.filterActual(actualParameters);
		cComp.filterActual(actualParameters);
		// aComp.filterActual(actualParameters);
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
	 * Retrieves all tuples (r,_,_,_) or (_,r,_,_) from sharing information.
	 * 
	 * @param r
	 * @return
	 */
	private ArrayList<Trio<Register,FieldSet,FieldSet>> getSinfo(Register r) {
		return sComp.findTuplesByRegister(r);
	}

	/**
	 * Retrieves all tuples (r,_) from cyclicity information.
	 * 
	 * @param r
	 * @return
	 */
	private ArrayList<FieldSet> getCinfo(Register r) {
		return cComp.findTuplesByRegister(r);
	}

	/**
	 * Retrieves all registers related to r from definite aliasing information.
	 * 
	 * @param r
	 * @return
	 */
	private ArrayList<Register> getAinfo(Register r) {
		return aComp.findTuplesByRegister(r);
	}
	
	/**
	 * Returns whether r is included in he purity information, i.e., if it may
	 * be impure.
	 * 
	 * @param r
	 * @return
	 */
	private boolean getPinfo(Register r) {
		return pComp.contains(r);
	}

	/**
	 * Returns true iff both sharing and cyclicity information is empty (no tuples). 
	 * 
	 * @return
	 */
	public boolean isBottom() {
		return sComp.isBottom() && cComp.isBottom() && aComp.isBottom() && pComp.isBottom();
	}

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
		avIp.copyCInfo(base,dest);
		// copy cyclicity of base into self-"reachability" of dest (PAPER: not in JLAMP paper)
		avIp.copySInfoFromC(base,dest);
		// copy self-sharing of base into self-sharing of dest, also removing the field
		for (Pair<FieldSet,FieldSet> p : getSinfo(base,base)) {
			if (p.val0.contains(field) && p.val1.contains(field)) {
				FieldSet fs0 = FieldSet.removeField(p.val0,field);
				FieldSet fs1 = FieldSet.removeField(p.val1,field);
				avIp.addSInfo(dest,dest,p.val0,p.val1);
				// PAPER: this was not in JLAMP (I guess)
				if (!(p.val0 == p.val1 && p.val0 == FieldSet.addField(FieldSet.emptyset(),field) && !hasNonTrivialCycles(base))) {
					avIp.addSInfo(dest,dest,fs0,p.val1);
					avIp.addSInfo(dest,dest,p.val0,fs1);
				}
				avIp.addSInfo(dest,dest,fs0,fs1);
			}
    		}
		int m = entry.getNumberOfReferenceRegisters();
		ArrayList<Register> defAlias = getAinfo(dest);
		for (int i=0; i<m; i++) {
			Register w = entry.getNthReferenceRegister(i);
			// WARNING PAPER: the use of definite aliasing was not in the paper: this is a
			// matter of optimization, but check if Definite Aliasing really makes
			// a difference
			if (!defAlias.contains(w)) {
				for (Pair<FieldSet,FieldSet> p : getSinfo(base,w)) {
					// according to the definition of the \ominus operator
					if (p.val0.contains(field)) {
						FieldSet fsl1 = FieldSet.removeField(p.val0,field);
						avIp.addSInfo(dest,w,p.val0,p.val1);
						avIp.addSInfo(dest,w,fsl1,p.val1);
						// according to the definition of the \oplus operator
						if (p.val0 == FieldSet.emptyset()) { 
							FieldSet fsr = FieldSet.addField(p.val1,field);
							avIp.addSInfo(dest,w,p.val0,fsr);
							avIp.addSInfo(dest,w,fsl1,fsr);    				
						}
					}
				}
			}
		}
		// purity
		avIp.copyPInfo(base,dest);
		// final abstract value
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
	 * Produces a new {@code TuplesAbstractValue} object representing the abstract
	 * information after a putfield {@code Quad} {@code q}, given {@code this}
	 * as the initial information.
	 * <p>
	 * The computation is taken from both TOCL (the cyclicity part) and JLAMP
	 * (the sharing part, sometimes limited to reachability)
	 */
	public TuplesAbstractValue doPutfield(Entry entry, joeq.Compiler.Quad.Quad q, Register v,
			Register rho, jq_Field field) {
		TuplesAbstractValue avIp = clone();
		// this optimization takes purity into account: v.f = null means a
		// change in the data structure if v.f was not null before
		if (isNull(rho)) {
			Utilities.info("UPDATING A FIELD WITH NULL");
			if (!isNull(rho,field)) { // data structure is modified
				for (Trio<Register,FieldSet,FieldSet> t : avIp.getSinfo(v)) {
					Register r = t.val0;
					if (!avIp.getPinfo(r)) {
						avIp.addPInfo(r);
						Utilities.info("REGISTER " + r + " MARKED AS IMPURE");
					}
				}
				return avIp;
			}
		}
		int m = entry.getNumberOfReferenceRegisters();

		// I''_s
		TuplesAbstractValue avIpp = new TuplesAbstractValue();
		// computing Z
		FieldSet z1 = FieldSet.addField(FieldSet.emptyset(),field);
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
    	    						avIpp.addSInfo(w1,w2,fsL_a,fsR_a);
    	    						for (FieldSet z2_fs : z2)
    	    							avIpp.addSInfo(w1,w2,FieldSet.union(fsL_a,z2_fs),fsR_a);
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
    	    						avIpp.addSInfo(w1,w2,fsL_b,fsR_b);
    	    						for (FieldSet z2_fs : z2)
    	    							avIpp.addSInfo(w1,w2,fsL_b,FieldSet.union(fsR_b,z2_fs));
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
    	    							avIpp.addSInfo(w1,w2,fsL_c,fsR_c);
    	    							for (FieldSet z2_fs1 : z2)
    	    								for (FieldSet z2_fs2 : z2)
    	    									avIpp.addSInfo(w1,w2,FieldSet.union(fsL_c,z2_fs1),FieldSet.union(fsR_c,z2_fs2));
    	    						}
    	    					}
    	    				}
				}
    			}
    		}    	
		// cyclicity
		// PAPER: there seems to be an unsoundness issue in TOCL14: cyclicity
		// should also be updated if I'_r(w,rho) \neq false (not only if
		// I'_r(w,v) \neq false); however, the former implies the latter
		// because, if w reaches rho, and some new cycle is created, then rho
		// has to reach v, so that, in the end, w also reach v.
		for (int i=0; i<m; i++) {
			Register w = entry.getNthReferenceRegister(i);
			boolean reaches = false;
			for (Pair<FieldSet,FieldSet> w_to_v : avIp.getSinfo(w,v))
				reaches |=  (w_to_v.val1 == FieldSet.emptyset());
			if (reaches) {
				avIpp.copyCInfo(rho,w);
				for (Pair<FieldSet,FieldSet> rho_to_v : avIp.getSinfo(rho,v)) {
					if (rho_to_v.val1 == FieldSet.emptyset()) { // reachability from rho to v
						FieldSet fs = FieldSet.addField(rho_to_v.val0,field);
						avIpp.addCInfo(w,fs);						
					}
				}
				avIpp.copyCinfoFrom(this,rho,w);
			}
		}
		// definite aliasing: information is taken from I'_s
		avIpp.copyAinfoFrom(avIp);
		// purity: each register possibly (and field-insensitively) sharing with
		// v is marked as impure
		avIpp.pComp = avIp.pComp.clone();
		for (Trio<Register,FieldSet,FieldSet> t : avIp.getSinfo(v)) {
			Register r = t.val0;
			if (!avIpp.getPinfo(r)) {
				avIpp.addPInfo(r);
				Utilities.info("REGISTER " + r + " MARKED AS IMPURE");
			}
		}
		// final abstract value
		avIp.updateInfo(avIpp);
		if (v.isTemp()) avIp.removeInfo(v);
		return avIp;
	}

	public TuplesAbstractValue doInvoke(Entry entry, Entry invokedEntry,
			joeq.Compiler.Quad.Quad q, ArrayList<Register> actualParameters,Register rho) {
		// copy of I_s
    		TuplesAbstractValue avIp = clone();
    		// WARNING: this line removed because it is not clear why it was here
    		// current purity information is not passed to the invoked entry 
    		// avIp.clearPurity();
    		// only actual parameters are kept; av_Ip becomes I'_s in the paper
    		avIp.filterActual(entry,actualParameters);
    		Utilities.info("I'_s = " + avIp);
    	
    		// this produces I'_s[\bar{v}/mth^i] in the paper, where the abstract information
    		// is limited to the formal parameters of the invoked entry
    		avIp.actualToFormal(actualParameters,invokedEntry);
    		// I'_s[\bar{v}/mth^i] is used to update the input of the summary of the invoked entry;
    		// if the new information is not included in the old one, then the invoked entry needs
    		// to be re-analyzed
    		if (GlobalInfo.getSummaryManager().updateSummaryInput(invokedEntry,avIp)) GlobalInfo.wakeUp(invokedEntry);
    		// this is \absinterp(mth)(I'_s[\bar{v}/mth^i]), although, of course,
    		// the input and output components of a summary are not "synchronized"
    		// (we've just produced a "new" input, but we are still using the "old"
    		// output while waiting for the entry to be re-analyzed)
    		TuplesAbstractValue avIpp;
    		if (GlobalInfo.bothImplementations())
    			avIpp = ((BothAbstractValue) GlobalInfo.getSummaryManager().getSummaryOutput(invokedEntry)).getTuplesPart().clone();
    		else avIpp = ((TuplesAbstractValue) GlobalInfo.getSummaryManager().getSummaryOutput(invokedEntry)).clone();
    		// this generates I''_s, which could be empty if no summary output is available
    		Utilities.info("SUMMARY OUTPUT = " + avIpp);
    		if (avIpp != null) {
    			avIpp.cleanGhostRegisters(invokedEntry);
    			avIpp.formalToActual(actualParameters,rho,invokedEntry);
    		} else avIpp = new TuplesAbstractValue();
    		Utilities.info("I''_s = " + avIpp);
    		
    		// start computing I'''_s
    		Utilities.begin("COMPUTING I'''_s");
    		TuplesAbstractValue avIppp = new TuplesAbstractValue();
    		// purity is taken from I'' and propagated to sharing registers
    		for (PurityTuple impure : avIpp.getPComp().getInfo()) {
    			Register rimpure = impure.getR();
    			for (Trio<Register,FieldSet,FieldSet> t : this.getSinfo(rimpure)) {
    				Register r = t.val0;
    				if (!avIppp.pComp.contains(r)) {
    					avIppp.addPInfo(r);
    					Utilities.info("REGISTER " + r + " MARKED AS IMPURE");
    				}
    			}
    		}
    		// sharing
    		int m = entry.getNumberOfReferenceRegisters();
    		int n = actualParameters.size();
    		TuplesAbstractValue[][] avs_sh = new TuplesAbstractValue[n][n];
    		// computing each I^{ij}_s
    		for (int i=0; i<n; i++) {
    			for (int j=0; j<n; j++) {
    				avs_sh[i][j] = new TuplesAbstractValue();
    				// WARNING: can possibly filter out non-reference registers
    				Register vi = actualParameters.get(i);
    				Register vj = actualParameters.get(j);
    				// where purity information is taken into account
    				if (avIpp.getPinfo(vi) || avIpp.getPinfo(vj)) {
    					for (int k1=0; k1<m; k1++) { // for each w_1 
    						for (int k2=0; k2<m; k2++) { // for each w_2
    							Register w1 = entry.getNthReferenceRegister(k1);
    							Register w2 = entry.getNthReferenceRegister(k2);
    							for (Pair<FieldSet,FieldSet> pair_1 : getSinfo(w1,vi)) { // \omega_1(toRight)
    								for (Pair<FieldSet,FieldSet> pair_2 : getSinfo(vj,w2)) { // \omega_2(toLeft)
    									for (Pair<FieldSet,FieldSet> pair_ij : avIpp.getSinfo(vi,vj)) { // \omega_ij
    										for (Pair<FieldSet,FieldSet> newPairs : getNewPairs(pair_1,pair_2,pair_ij))
    											avs_sh[i][j].addSInfo(w1,w2,newPairs.val0,newPairs.val1);
    									}                    					
    								}
    							}
    						}
    					}
    				} else Utilities.info("IGNORING vi=" + vi + ", vj=" + vj + ": THEY ARE BOTH PURE");

    			}
    		}
    		// joining all together into I'''_s
    		for (int i=0; i<n; i++)
    			for (int j=0; j<n; j++)
    				avIppp.updateSInfo(avs_sh[i][j]);
    		// cyclicity
    		TuplesAbstractValue[] avs_cy = new TuplesAbstractValue[n];
    		for (int i=0; i<n; i++) {
    			Register vi = actualParameters.get(i);
    			avs_cy[i] = new TuplesAbstractValue();
    			if (avIpp.getPinfo(vi)) // vi is impure
    				for (int j=0; j<m; j++) {
    					Register w = entry.getNthReferenceRegister(j);
    					if (!getSinfo(w,vi).isEmpty()) { // some kind of sharing
    						Utilities.info(w + " SHARING WITH " + vi);
    						avs_cy[i].copyCinfoFrom(avIpp,vi,w);
    					}
    				}
    			avIppp.updateCInfo(avs_cy[i]);
    		}
    		// finishing
    		avIppp.removeActualParameters(actualParameters);
    		Utilities.end("COMPUTING I'''_s = " + avIppp);

    		// computing I''''_s
    		Utilities.begin("COMPUTING I''''_s");
    		TuplesAbstractValue avIpppp = new TuplesAbstractValue();
    		if (rho != null) {
    			Utilities.info("METHOD WITH RETURN VALUE " + rho);
    			// sharing
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
    									avIpppp.addSInfo(rho,w,fs,omega0.val1);
    								}
    							}
    				}
    			}
    			// cyclicity
    			for (int k=0; k<n; k++) {
    				Register vk = actualParameters.get(k);
    				boolean reaches = false;
    				for (Pair<FieldSet,FieldSet> p : avIpp.getSinfo(vk,rho))
    					reaches |= p.val1 == FieldSet.emptyset();
    				if (reaches) {
    					ArrayList<FieldSet> fss = this.getCinfo(vk);
    					for (FieldSet fs : fss) avIpppp.addCInfo(rho,fs);
    				}
    				
    			}
    		} else Utilities.info("METHOD WITH NO RETURN VALUE");
    		Utilities.end("COMPUTING I''''_s = " + avIpppp);

    		// computing the final union I_s \/ I''_s \/ I'''_s \/ I''''_s
    		TuplesAbstractValue avOut = clone();
    		avOut.removeActualParameters(actualParameters);
    		// WARNING: think if this avIpp can be removed (it can't for the moment, otherwise the return value has no input attached)
    		// PAPER: this was in the paper, but it could be imprecise
    		avOut.updateInfo(avIpp);
    		avOut.updateInfo(avIppp);
    		avOut.updateInfo(avIpppp);
    		// purity information is taken directly from the initial abstract value
    		// avOut.updatePurity(this);
    		Utilities.info("FINAL UNION: " + avOut);
    		return avOut;
	}

	/**
	 * Auxiliary method of {@code doInvoke}.
	 * 
	 * @param pair_1
	 * @param pair_2
	 * @param pair_ij
	 * @return
	 */
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
	
	/**
	 * Reorders the abstract information.
	 */
	public void sort() {
		sComp.sort();
		cComp.sort();
		aComp.sort();
		pComp.sort();
	}
	
	/**
	 * Returns true iff there is no cyclicity information associated with {@code r},
	 * which means that it is definitely null.  Cyclicity is used instead of sharing
	 * because it is simpler.
	 * 
	 * @param r
	 * @return
	 */
	private boolean isNull(Register r) {
		return getCinfo(r).isEmpty();
	}

	/**
	 * Returns true iff there is no sharing information associated with {@code r.f},
	 * which means that it is definitely null.
	 * 
	 * @param r
	 * @return
	 */
	private boolean isNull(Register r,jq_Field field) {
		ArrayList<Pair<FieldSet,FieldSet>> sh = getSinfo(r,r);
		FieldSet fs = FieldSet.addField(FieldSet.emptyset(),field);
		for (Pair<FieldSet, FieldSet> t : sh) {
			if (t.val0 == fs && t.val1 == fs) return false;
		}
		return true;
	}

	public String toString() {
		return sComp.toString() + " / " + cComp.toString() + " / "
				+ aComp.toString() + " / " + pComp.toString();
	}

}
