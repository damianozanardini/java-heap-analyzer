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
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;


// WARNING: should put some sort of order on Registers in order to avoid redundancies 
public class TuplesAbstractValue extends AbstractValue {

	private STuples sComp;
	private CTuples cComp;
	
	public TuplesAbstractValue() {
		sComp = new STuples();
		cComp = new CTuples();
	}
	
	protected TuplesAbstractValue(STuples st,CTuples ct) {
		sComp = st;
		cComp = ct;
	}
	
	public boolean update(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof TuplesAbstractValue)
			return (sComp.join(((TuplesAbstractValue) other).getSComp()) | cComp.join(((TuplesAbstractValue) other).getCComp()));
		// should never happen
		else return false;
	}
	
	/**
	 * Returns the sharing component of the abstract value
	 * 
	 * @return
	 */	
	protected STuples getSComp() {
		return sComp;
	}
	
	/**
	 * Returns the cyclicity component of the abstract value
	 * 
	 * @return
	 */
	protected CTuples getCComp() {
		return cComp;
	}
		
	protected void setSComp(STuples stuples){
		this.sComp = stuples;
	}
	
	protected void setCComp(CTuples ctuples){
		this.cComp = ctuples;
	}

	/**
	 * Returns a new AbstractValue object with the same abstract information.
	 * The copy is neither completely deep nor completely shallow: for example,
	 * Register objects are not duplicated.
	 * 
	 * @return a copy of itself
	 */
	public TuplesAbstractValue clone() {
		return new TuplesAbstractValue(sComp.clone(),cComp.clone());
	}
	
	/**
	 * In tuples, renames actual parameters into the corresponding formal parameters  
	 */
	public void actualToFormal(List<Register> apl,Entry e) {
		Utilities.begin("ACTUAL " + apl + " TO FORMAL FROM " + this);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				dest.add(e.getNthReferenceRegister(i));
				// dest.add(RegisterManager.getRegFromNumber(m,i));
				source.add(apl.get(i));
			} catch (IndexOutOfBoundsException exc) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		sComp.moveTuplesList(source,dest);
		cComp.moveTuplesList(source,dest);
		Utilities.end("ACTUAL " + apl + " TO FORMAL RESULTING IN " + this);
	}
	
	/**
	 * In tuples, renames formal parameters into the corresponding actual parameters  
	 */
	public void formalToActual(List<Register> apl, Entry e) {
		Utilities.begin("FORMAL FROM " + this + " TO ACTUAL "+ apl);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				source.add(e.getNthReferenceRegister(i));
				// source.add(RegisterManager.getRegFromNumber(m,i));
				dest.add(apl.get(i));
			} catch (IndexOutOfBoundsException exc) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		sComp.moveTuplesList(source,dest);
		cComp.moveTuplesList(source,dest);
		Utilities.end("FORMAL TO ACTUAL " + apl + " RESULTING IN " + this);
	}
	
	public void addSinfo(Register r1,Register r2,FieldSet fs1,FieldSet fs2) {
		getSComp().addTuple(r1, r2, fs1, fs2);
	}
	
	public void addCinfo(Register r,FieldSet fs) {
		getCComp().addTuple(r,fs);
	}
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * @param source The source variable.
     * @param dest The destination variable.
     * @return
     */
    public void copyInfo(Register source,Register dest) {
    	sComp.copyTuples(source,dest);
    	cComp.copyTuples(source,dest);
    }
    
    public void copySinfo(Register source,Register dest) {
    	sComp.copyTuples(source,dest);
    }
    
    public void copyCinfo(Register source,Register dest) {
    	cComp.copyTuples(source,dest);
    }
    
    public void moveInfo(Register source,Register dest) {
    	sComp.moveTuples(source,dest);
    	cComp.moveTuples(source,dest);
    }
	
    public void moveSinfo(Register source,Register dest) {
    	sComp.moveTuples(source,dest);
    }

    public void moveCinfo(Register source,Register dest) {
    	sComp.moveTuples(source,dest);
    }

    public void moveInfoList(List<Register> source,List<Register> dest) {
    	sComp.moveTuplesList(source,dest);
    	cComp.moveTuplesList(source,dest);
    }
    
    public void copyFromCycle(Register source,Register dest) {
    	sComp.copyTuplesFromCycle(source,dest,cComp);
    }

    public void removeInfo(Register r) {
    	sComp.remove(r);
    	cComp.remove(r);
    }
    
	public void removeInfoList(List<Register> rs) {
		sComp.removeList(rs);
    	cComp.removeList(rs);
	}
        
	public void copyToGhostRegisters(Entry entry) {
		jq_Method method = entry.getMethod();
		Utilities.begin("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(method));
		for (Register r : entry.getReferenceRegisterList()) {
			if (!r.getType().isPrimitiveType()) {
				Register ghost = GlobalInfo.getGhostCopy(method,r);
				if (ghost!=null) copyInfo(r,ghost);
			}
		}
		Utilities.end("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(method));
	}

	public void cleanGhostRegisters(Entry entry) {
		Utilities.begin("CLEANING GHOST INFORMATION");
		jq_Method method = entry.getMethod();
		for (Register r : entry.getReferenceFormalParameters()) {
			if (!r.getType().isPrimitiveType() && !GlobalInfo.isGhost(method,r)) {
				Register rprime = GlobalInfo.getGhostCopy(method,r);
				removeInfo(r);
				moveInfo(rprime,r);
			}
		}
		Utilities.end("CLEANING GHOST INFORMATION");
	}

	/**
	 * Removes from tuples all registers which are not actual parameters
	 * 
	 * @param actualParameters the list of actual parameters (not exactly a list of
	 * registers, but the Register object can be retrieved easily)
	 */
	public void filterActual(List<Register> actualParameters) {
		Utilities.begin("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");
		sComp.filterActual(actualParameters);
		cComp.filterActual(actualParameters);
		Utilities.info("NEW AV: " + this);
		Utilities.end("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");		
	}

	private ArrayList<Pair<FieldSet,FieldSet>> getSinfo(Register r1,Register r2) {
		return sComp.findTuplesByBothRegisters(r1, r2);
	}

	private ArrayList<Pair<Register,FieldSet>> getSinfoReachingRegister(Register r) {
		return sComp.findTuplesByReachingRegister(r);
	}

	private ArrayList<Pair<Register,FieldSet>> getSinfoReachedRegister(Register r) {
		return sComp.findTuplesByReachedRegister(r);
	}
	
	private ArrayList<FieldSet> getSinfoReachingReachedRegister(Register r1, Register r2) {
		return sComp.findTuplesByReachingReachedRegister(r1,r2);
	}

	private ArrayList<Trio<Register,FieldSet,FieldSet>> getSinfoFirstRegister(Register r) {
		return sComp.findTuplesByFirstRegister(r);
	}

	private ArrayList<Trio<Register,FieldSet,FieldSet>> getSinfoSecondRegister(Register r) {
		return sComp.findTuplesBySecondRegister(r);
	}

	private ArrayList<FieldSet> getCinfo(Register r) {
		return cComp.findTuplesByRegister(r);
	}
	
	public String toString() {
		return sComp.toString() + " / " + cComp.toString();
	}

	public boolean isBottom() {
		return sComp.isBottom() && cComp.isBottom();
	}

	public TuplesAbstractValue propagateGetfield(Entry entry, joeq.Compiler.Quad.Quad q, Register base,
			Register dest, jq_Field field) {
    	// verifying if base.field can be non-null; if not, then no new information is
    	// produced because dest is certainly null
    	// WARNING PAPER: add this to the paper?
    	List<Pair<FieldSet,FieldSet>> selfSH = getSinfo(base,base);
    	boolean shareOnField = false;
    	for (Pair<FieldSet,FieldSet> p : selfSH) {
    		shareOnField |= (p.val0 == FieldSet.addField(FieldSet.emptyset(),field) &&
    			p.val1 == p.val0);    			
    	}
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
    		// A solution would be to implement a simple DEFINITE ALIASING analysis
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
		for (FieldSet fs : cycles)
			b |= fs!=FieldSet.emptyset();
		return b;
	}

	public TuplesAbstractValue propagatePutfield(Entry entry, joeq.Compiler.Quad.Quad q, Register v,
			Register rho, jq_Field field) {
		TuplesAbstractValue avIp = clone();
		int m = entry.getNumberOfReferenceRegisters();
    	// I''_s
    	TuplesAbstractValue avIpp = new TuplesAbstractValue();

    	FieldSet z1 = FieldSet.addField(FieldSet.emptyset(),field);
    	// computing Z
    	List<Pair<FieldSet,FieldSet>> mdls_rhov = avIp.getSinfo(rho,v);
		ArrayList<FieldSet> z2 = new ArrayList<FieldSet>();
    	for (Pair<FieldSet,FieldSet> p : mdls_rhov) {
    		if (p.val1 == FieldSet.emptyset())
    			z2.add(p.val0);
    	}
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
    	// WARNING: cyclicity currently missing
    	avIp.update(avIpp);
    	return avIp;
	}

	public TuplesAbstractValue propagateInvoke(Entry entry, Entry invokedEntry,
			joeq.Compiler.Quad.Quad q, ArrayList<Register> actualParameters) {
		// copy of I_s
    	TuplesAbstractValue avIp = clone();
    	// only actual parameters are kept; av_Ip becomes I'_s in the paper
    	avIp.filterActual(actualParameters);
    	Utilities.info("I'_s = " + avIp);
    	
    	// this produces I'_s[\bar{v}/mth^i] in the paper, where the abstract information
    	// is limited to the formal parameters of the invoked entry
    	avIp.actualToFormal(actualParameters,invokedEntry);
    	// I'_s[\bar{v}/mth^i] is used to update the input of the summary of the invoked entry;
    	// if the new information is not included in the old one, then the invoked entry needs
    	// to be re-analyzed
    	boolean needToWakeUp = GlobalInfo.summaryManager.updateSummaryInput(invokedEntry,avIp);
    	if (needToWakeUp) GlobalInfo.wakeUp(invokedEntry);
    	// this is \absinterp(mth)(I'_s[\bar{v}/mth^i]), although, of course,
    	// the input and output components of a summary are not "synchronized"
    	// (we've just produced a "new" input, but we are still using the "old"
    	// output while waiting the entry to be re-analyzed)
    	TuplesAbstractValue avIpp = null;
    	if (GlobalInfo.bothImplementations())
    		avIpp = ((BothAbstractValue) GlobalInfo.summaryManager.getSummaryOutput(invokedEntry)).getTuplesPart();
    	else avIpp = ((TuplesAbstractValue) GlobalInfo.summaryManager.getSummaryOutput(invokedEntry));
    	// this generates I''_s, which could be empty if no summary output is available
    	//
    	// WARNING: have to take the return value into account
    	if (avIpp != null) {
    		avIpp.cleanGhostRegisters(invokedEntry);
    		avIpp.formalToActual(actualParameters,invokedEntry);
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
    	
    	// computing the final union I_s \vee I'_s \vee I''_s \vee I'''_s
    	// WARNING: I''''_s is still not here
    	TuplesAbstractValue avOut = clone();
    	avOut.removeInfoList(actualParameters);
    	avOut.update(avIpp);
    	avOut.update(avIppp);
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

}