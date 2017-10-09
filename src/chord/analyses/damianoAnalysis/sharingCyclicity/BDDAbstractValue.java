package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.BDDIterator;
import net.sf.javabdd.BDDBitVector;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;


// WARNING: for the time being, cyclicity is not supported
public class BDDAbstractValue extends AbstractValue {
	private Entry entry;
	
	private ShBDD sComp;
			
	static final int SHARE = 0;
	static final int CYCLE = 1;
	static final int LEFT = -1;
	static final int UNIQUE = 0;
	static final int RIGHT = 1;
	
	/**
	 * Main constructor, creates a BDDAnstractValue object based on the
	 * entry information
	 * 
	 * @param entry the entry object needed to collect the information related
	 * to registers and fields
	 */
	public BDDAbstractValue(Entry e) {
		entry = e;
		sComp = new ShBDD();
	}

    public BDDAbstractValue(Entry e, BDD sc) {
		entry = e;
		sComp = new ShBDD(sc);
	}

    public BDDAbstractValue(Entry e, ShBDD sc) {
		entry = e;
		sComp = sc;
	}

	// WARNING: double-check it when the bdd implementation will work on its own
	public boolean updateSInfo(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof BDDAbstractValue) {
			ShBDD sNew = ((BDDAbstractValue) other).getSComp();
			return sComp.update(sNew);
		} else {
			Utilities.err("BDDAbstractValue.update: wrong type of parameter - " + other);
			return false;
		}
	}
		
	public boolean updateCInfo(AbstractValue other) {
		// TODO
		return false;
	}
		
	// WARNING: double-check it when the bdd implementation will work on its own
	public boolean updateAInfo(AbstractValue other) {
		return false;
	}
		
	// WARNING: double-check it when the bdd implementation will work on its own
	public boolean updatePInfo(AbstractValue other) {
		return false;
	}
		
	public void clearPInfo() {
		// TODO
	}
	
	/**
	 * Returns a new BDDAbstractValue object with the same abstract information.
	 * 
	 * @return a copy of itself
	 */
	public BDDAbstractValue clone() {
		return new BDDAbstractValue(entry, sComp.clone());
	}
		
	/**
	 * Generates a new BDD describing the sharing information between the registers and
	 * fieldsets provided as parameters, and "adds" it (by logical disjunction)
	 * to the existing information
	 * 
	 * @param r1 the first register
	 * @param r2 the second register
	 * @param fs1 the fieldset associated to r1
	 * @param fs2 the fieldset associated to r2
	 */
	public void addSInfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		sComp.addInfo(r1,r2,fs1,fs2);
	}

	public void addCInfo(Register r, FieldSet fs) {
		// TODO
	}

	public void addAInfo(Register r1,Register r2) {
		// TODO
	}
	
	public void addPInfo(Register r) {
		// TODO
	}

	/**
	 * Copies the Cyclicity and Sharing information from one register to another
	 * and keeps both in the existing information.
	 * 
	 * @param source the original register to get the information to be copied
	 * @param dest the destination register for the information.
	 */
	public void copyInfo(Register source, Register dest) {
		copySInfo(source,dest);
		copyCInfo(source,dest);
	}
	
	// WARNING: from (source,source,fs1,fs2) both (dest,source,fs1,fs2) and 
	// (source,dest,fs1,fs2) are produced; this is redundant, but we can live with it
	public void copySInfo(Register source, Register dest) {
		sComp = sComp.copy(source,dest);
	}

	public void copyCInfo(Register source, Register dest) {
		// TODO
	}

	public void copyAInfo(Register source, Register dest) {
		// TODO
	}
	
	public void copyPInfo(Register source, Register dest) {
		// TODO
	}

	/**
	 * Moves the Cyclicity and Sharing information from one register to another.
	 * All the information referencing the source register will be deleted.
	 * 
	 * @param source the original register to get the information to be moved
	 * @param dest the destination register for the information.
	 */
	public void moveInfo(Register source, Register dest) {
		moveSInfo(source, dest);
		moveCInfo(source, dest);
		moveAInfo(source, dest);
		movePInfo(source, dest);
	}

	/**
	 * Moves the Sharing information from one register to another.
	 * All the information about the source register will be deleted.
	 * 
	 * @param source the original (source) register from which the information is moved
	 * @param dest the destination register
	 */
	public void moveSInfo(Register source, Register dest) {
		sComp.renameInPlace(source,dest);
	}

	public void moveCInfo(Register source, Register dest) {
		// TODO
	}

	public void moveAInfo(Register source, Register dest) {
		// TODO
	}

	public void movePInfo(Register source, Register dest) {
		// TODO
	}

	/**
	 * Removes the sharing and cyclicity information about a register. 
	 */
	public void removeInfo(Register r) {
		removeSInfo(r);
		removeCInfo(r);
		removeAInfo(r);
		removePInfo(r);
	}

	/**
	 * Removes the sharing information about a register. 
	 */
	public void removeSInfo(Register r) {
		sComp.removeInPlace(r);
	}
	
	/**
	 * Removes the cyclicity information about a register. 
	 */
	public void removeCInfo(Register r) {
		// TODO
	}
	
	/**
	 * Removes the definite aliasing information about a register. 
	 */
	public void removeAInfo(Register r) {
		// TODO
	}

	/**
	 * Removes the purity information about a register. 
	 */
	public void removePInfo(Register r) {
		// TODO
	}

	private void printLines() {
		sComp.printLines();
	}
	
	public String toString() {
		return sComp.toString();
	}
		
	public boolean equals(AbstractValue av) {
		if (av instanceof BDDAbstractValue)
		{
			boolean checkSComp = sComp.equals(((BDDAbstractValue) av).getSComp());
			boolean checkCComp = true; // TODO
			return checkSComp && checkCComp;
		}
		else return false;
	}
	
	public boolean isTop() {
		return sComp.isTop(); // TODO && cComp.isTop();
	}

	public boolean isBottom() {
		return sComp.isBottom(); // TODO && cComp.isBottom();
	}

	public void copySInfoFromC(Register base, Register dest) {
		// TODO
	}
		
	public BDDAbstractValue doGetfield(Quad q, Register base,
			Register dest, jq_Field field) {
		// TODO
		
		// case (a)
		ArrayList<jq_Field> leftList = new ArrayList<jq_Field>();
		leftList.add(field);
		ArrayList<jq_Field> rightList = new ArrayList<jq_Field>();
		rightList.add(field);
		ShBDD avIa = sComp.clone().restrictOnBothRegisters(base,base).pathDifference(leftList,rightList).existLR().restrictOnBothRegisters(dest,dest);
		
		// case (b)
		rightList.clear();
		ShBDD avIb = sComp.clone().restrictOnFirstRegister(base).pathDifference(leftList,rightList).existL().restrictOnFirstRegister(dest);
		
		// case (c)
		leftList.clear();
		rightList.add(field);
		ShBDD avIc = sComp.clone().restrictOnSecondRegister(base).pathDifference(leftList,rightList).existR().restrictOnSecondRegister(dest);
		
		ShBDD x = sComp.clone();
		x.orInPlace(avIa);
		x.orInPlace(avIb);
		x.orInPlace(avIc);
				
		return new BDDAbstractValue(entry,x);
	}

	public BDDAbstractValue doPutfield(Quad q, Register v, Register rho, jq_Field field) {
		
		ShBDD z_left = ShBDD.pathFormulaToBDD(FieldSet.addField(FieldSet.emptyset(),field),FieldSet.emptyset()).restrictOnBothRegisters(v,rho);
		ShBDD z_left2 = sComp.clone().restrictOnBothRegisters(rho,v).exist(ShBDD.fieldToBDD(field,LEFT));
		z_left2.andInPlace(new ShBDD(ShBDD.fieldToBDD(field,LEFT).and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),RIGHT)).getData()));
		z_left2.existLRInPlace();
		z_left.orInPlace(z_left2.restrictOnBothRegisters(v,rho));
		
		ShBDD z_right = ShBDD.pathFormulaToBDD(FieldSet.emptyset(),FieldSet.addField(FieldSet.emptyset(),field)).restrictOnBothRegisters(rho,v);
		ShBDD z_right2 = sComp.clone().restrictOnBothRegisters(v,rho).exist(ShBDD.fieldToBDD(field,RIGHT));
		z_right2.andInPlace(new ShBDD(ShBDD.fieldToBDD(field,RIGHT).and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),LEFT)).getData()));
		z_right2.existLRInPlace();
		z_right.orInPlace(z_right2.restrictOnBothRegisters(rho,v));

		// case (a)
		ShBDD xa = sComp.restrictOnSecondRegister(v).and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),RIGHT));
		ShBDD avIa = xa.concat(z_left).concat(sComp.restrictOnFirstRegister(rho));
		xa.free();
		
		// case (b)
		ShBDD xb = sComp.restrictOnFirstRegister(v).and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),LEFT));
		ShBDD avIb = sComp.restrictOnSecondRegister(rho).concat(z_right).concat(xb);
		xb.free();
		
		// case (c)
		ShBDD xc1 = sComp.restrictOnSecondRegister(v).and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),RIGHT));
		ShBDD xc2 = sComp.restrictOnFirstRegister(v).and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),LEFT));
		ShBDD avIc = xc1.concat(z_left).concat(sComp.restrictOnBothRegisters(rho,rho)).concat(z_right).concat(xc2);
		xc1.free();
		xc2.free();
				
		ShBDD x = sComp.clone();
		x.orInPlace(avIa);
		x.orInPlace(avIb);
		x.orInPlace(avIc);
		return new BDDAbstractValue(entry,x.remove(rho));
		
		/*BDDFactory bf = getOrCreateFactory(entry)[SHARE];
		BDDAbstractValue avIp = clone();

		// MIGUEL Falta Z empezaria aqui

	    FieldSet z1FS = FieldSet.addField(FieldSet.emptyset(),field);
	    BDD z1 = fieldSetToBDD(z1FS, RIGHT, SHARE);
	    BDD mdls_rhov = avIp.getSinfo(rho,v);
	    ArrayList<FieldSet> z2 = new ArrayList<FieldSet>();
    	
	    // termina aqui 
    	
		BDD bddIpp = bf.zero(); 
		// numero de registros así itero para todo w1 y w2
		int m = entry.getNumberOfReferenceRegisters();
		for (int i=0; i<m; i++) {
			for (int j=0; j<m; j++) {
				Register w1 = entry.getNthReferenceRegister(i);
				Register w2 = entry.getNthReferenceRegister(i);
				// case (a)
				BDD omega1A = getSinfo(w1, v).andWith(
    						fieldSetToBDD(FieldSet.emptyset(), RIGHT, SHARE));
				BDD omega2A	= getSinfo(rho, w2);
				BDD caseA = concatBDDs(omega1A, omega2A, SHARE);
				bddIpp.or(caseA);
				// case (b)
				BDD omega2B = getSinfo(v, w2).andWith(
						fieldSetToBDD(FieldSet.emptyset(), LEFT, SHARE));
				BDD omega1B =  getSinfo(w1, rho);
				BDD caseB = concatBDDs(omega1B, omega2B, SHARE);
				bddIpp.or(caseB);
				// case (c)
				// MIGUEL: optimizar porque algunos BDDS ya los tengo arriba.
				BDD omega1C =getSinfo(w1, v).andWith( 
						fieldSetToBDD(FieldSet.emptyset(), RIGHT, SHARE));
				BDD omegaC = getSinfo(rho, rho);
				BDD omega2C = getSinfo(v, w2).andWith(fieldSetToBDD(FieldSet.emptyset(), LEFT, SHARE));
				BDD caseC = concatBDDs(omega1C, concatBDDs(omegaC, omega2C, SHARE), SHARE);
				bddIpp.or(caseC);
			}
		}
		
		// MIGUEL: repasar ¿hace falta hacer esto? Lo he hecho como un clon de TuplesAV
		BDDAbstractValue avIpp = new BDDAbstractValue(entry, bddIpp, avIp.getCComp());
		// TODO Auto-generated method stub
		avIp.updateInfo(avIpp);
		return avIp;
		*/
	}

	public BDDAbstractValue doInvoke(Entry invokedEntry,
			Quad q, ArrayList<Register> actualParameters, Register returnValue) {
		BDDAbstractValue avI = clone();
		// I' (i.e., in principle, it contains all analysis data, although only sharing
		// is supported for the moment) WARNING keep this up-to-date
		BDDAbstractValue avIp = clone();
		avIp.filterActual(actualParameters);
		Utilities.info("I'_s = " + avIp);
		avIp.actualToFormal(actualParameters,invokedEntry);
		if (GlobalInfo.getSummaryManager().updateSummaryInput(invokedEntry,avIp)) GlobalInfo.wakeUp(invokedEntry);
		// this generates I'', which could be empty if no summary output is available
		BDDAbstractValue avIpp;
		if (GlobalInfo.bothImplementations())
			avIpp = ((BothAbstractValue) GlobalInfo.getSummaryManager().getSummaryOutput(invokedEntry)).getBDDPart().clone();
		else avIpp = ((BDDAbstractValue) GlobalInfo.getSummaryManager().getSummaryOutput(invokedEntry)).clone();
		Utilities.info("SUMMARY OUTPUT (I'') = " + avIpp);		
		
		ShBDD bdd = avI.sComp;
		ShBDD bddpp = avIpp.sComp;
		// I'''_s
		ShBDD bddppp = new ShBDD(); // a false BDD
		// I^ij_s
		for (Register vi : actualParameters) {
			for (Register vj : actualParameters) {
				// WARNING: purity information not considered here
				ShBDD bddij1 = bdd.restrictOnSecondRegister(vi).existR().and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),RIGHT));
				ShBDD bddij2 = bddpp.capitalF(bdd,vi,vj,0);
				ShBDD bddij3 = bdd.restrictOnFirstRegister(vj).existL().and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),LEFT));
				bddppp.orInPlace(bddij1.concat(bddij2).concat(bddij3));
			}
		}
		Utilities.info("I'''_s = " + bddppp);		
		
		// I''''_s
		ShBDD bddpppp = new ShBDD(); // a false BDD
		for (Register vi : actualParameters) {
			ShBDD bddi = bddpp.restrictOnBothRegisters(returnValue,vi).existR().and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),RIGHT));
			bddpppp.orInPlace(bddi.concat(bddpp.capitalHl(bdd,vi,returnValue,0)));
		}
		for (Register vi : actualParameters) {
			ShBDD bddi = bddpp.restrictOnBothRegisters(vi,returnValue).existL().and(ShBDD.fieldSetToBDD(FieldSet.emptyset(),LEFT));
			bddpppp.orInPlace(bddi.concat(bddpp.capitalHr(bdd,vi,returnValue,0)));
		}
	
		avI.sComp.update(bddppp);
		avI.sComp.update(bddpppp);
		return avI;
	}
	
	public ArrayList<Pair<FieldSet, FieldSet>> getStuples(Register r1, Register r2) {
		// TODO
		return null;
	}
	//		BDD sharing = sComp.restrictSharingOnBothRegisters(r1,r2).getData();
	//		ArrayList<BDD> list = separateSolutions(sharing,new int[nBDDVars_sh],SHARE);
	//		ArrayList<Pair<FieldSet, FieldSet>> pairs = new ArrayList<Pair<FieldSet, FieldSet>>();
	//		for (BDD b : list)
	//			pairs.add(bddToFieldSetPair(b));			
	//		return pairs;
	//}
	
	public ArrayList<FieldSet> getCtuples(Register r) {
		// TODO
		return null;
	}
	
	public ShBDD getSComp() {
		return sComp;
	}

	public void filterActual(List<Register> actualParameters) {
		// sharing
		Utilities.begin("KEEPING ONLY ACTUAL PARAMETERS " + actualParameters);
		sComp.filterActual(actualParameters);
		Utilities.end("KEEPING ONLY ACTUAL PARAMETERS " + actualParameters + ", RESULTING IN " + sComp);
		// cyclicity
		// TODO
	}
	
	
}
