package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.List;

import joeq.Class.jq_Field;
import chord.analyses.damianoAnalysis.Utilities;
import chord.project.ClassicProject;


/**
 * This class implements field sets.
 * A field set is represented bitwise.
 * If all program fields are tracked explicitly, then the n-th bit (from the
 * right) corresponds to the (n-1)-th element of the AbsF domain, which, in
 * this case, is also the (n-1)-th element of the F domain (recall that the
 * 0-th element of the F domain represents all arrays).
 * If, on the other hand, only some fields are tracked explicitly, then the
 * n-th bit (from the right) corresponds to the (n-1)-th element of the AbsF
 * domain, where the 0-th element (i.e., the first bit from the right in the
 * bitwise representation) represents the "*" field (NOTE: it was not possible,
 * in this case, to reserve the index 0 of AbsF to arrays and the index 1 to
 * "*", so only the index 0 is currently reserved).
 * @author Damiano Zanardini
 *
 */
public class FieldSet {
	
	/**
	 * This variables allows up to 32 fields to be dealt with. It would be great
	 * to use a long instead, and manage up to 64 fields; however, this seems
	 * impossible given the declaration of IndexMap.get() 
	 */
	private int val;
	
	/**
	 * Creates a field set directly from the bitwise representation.
	 * @param n The bitwise representation.
	 */
	public FieldSet(int n) {
		val = n;
	}
		
	/**
	 * Returns the bitwise representation.
	 * @return the bitwise representation.
	 */
	public int getVal() { return val; }
	
	/**
	 * Modifies a field set directly from the bitwise representation.
	 * @param n The bitwise representation.
	 */
	public void setVal(int n) { val = n; }
	
	/**
	 * Updates a field set by setting it to a singleton field set only
	 * containing the field passed as a parameter; if such a field is tracked
	 * explicitly, then the field set actually contains it; otherwise, the
	 * field set contains "*".
	 * @param fld The only field belonging to the field set.
	 */
	public void setVal(jq_Field fld) {
		DomAbsField fields = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		int id = fields.indexOf(fld);
		if (id >= 0) { // the field is tracked explicitly
			val = 1 << id;
		} else { // the "*" field
			val = 1;
		}
	}

	/**
	 * This method returns the empty field set.
	 * Note that it does NOT create a new field set; rather, it takes the 0-th
	 * element of the FSet domain.
	 * 
	 * @return The empty field set.
	 */
	public static FieldSet emptyset() {
		DomFieldSet fieldsets = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
		return fieldsets.get(0);
	}

	public static FieldSet getByVal(int val) {
		DomFieldSet fieldsets = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
		return fieldsets.get(val);
	}
	
	/**
	 * This method returns the union of two field sets.
	 * Note that it does NOT create a new field set; rather, it takes the
	 * corresponding element of the FSet domain.
	 * 
	 * @param fs1 The first field set.
	 * @param fs2 The second field set.
	 * @return The union of both field sets.
	 */
	public static FieldSet union(FieldSet fs1, FieldSet fs2) {
		DomFieldSet fieldsets = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
		return fieldsets.get(fs1.getVal() | fs2.getVal());
	}
	
	public static FieldSet setDifference(FieldSet fs1, FieldSet fs2) {
		DomFieldSet fieldsets = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
		return fieldsets.get(fs1.getVal() & ~fs2.getVal());
	}
	
	/**
	 * This method returns the union of field set {@code fs} given as a
	 * parameter, and a singleton field set containing {@code fld}.
	 * Note that it does NOT create a new field set; rather, it takes the
	 * corresponding element of the FSet domain.
	 * @param fs The initial field set.
	 * @param fld The only field belonging to the singleton field set.
	 * @return The union of both field sets.
	 */
	public static FieldSet addField(FieldSet fs, jq_Field fld) {
		DomAbsField fields = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		DomFieldSet fieldsets = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
		int id = fields.indexOf(fld);
		if (id >= 0) { // the field is tracked explicitly
			return fieldsets.get(fs.getVal() | (1 << id));
		} else { // the "*" field
			return fieldsets.get(fs.getVal() | 1);
		}
	}
	
	/**
	 * This method returns the set-difference of field set {@code fs} given as
	 * a parameter, and a singleton field set containing {@code fld}.
	 * Note that it does NOT create a new field set; rather, it takes the
	 * corresponding element of the FSet domain.
	 * If {@code fld} is not tracked explicitly, then, conservatively, "*" is
	 * not removed.
	 * @param fs The initial field set.
	 * @param fld The only field belonging to the singleton field set.
	 * @return The set-difference between both field sets.
	 */
	public static FieldSet removeField(FieldSet fs, jq_Field fld) {
		DomAbsField fields = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		DomFieldSet fieldsets = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
		int id = fields.indexOf(fld);
		if (id >= 0) { // the field is tracked explicitly
			return fieldsets.get(fs.getVal() & ~(1 << id));
		} else { // the "*" field
			return fieldsets.get(fs.getVal() & ~(1));
		}
	}
	
	/**
	 * This methods establishes an ordering between field sets, which is used
	 * in order not to duplicate self-f-sharing tuples.  The order is not meant
	 * to cope with any specific property of field sets: the "smaller" field
	 * set is not necessarily the one with less fields.  It is just a matter of
	 * having some ordering on field sets.
	 * 
	 * @param fs1 the first field set to compare
	 * @param fs2 the second field set to compare
	 * @return whether the bitwise representation of fs1 is less than or
	 * equal to fs2's
	 */
	public static boolean leq(FieldSet fs1, FieldSet fs2) {
		return (Integer.compare(fs1.getVal(),fs2.getVal()) <= 0);
	}
	
	public static FieldSet min(FieldSet fs1, FieldSet fs2) {
		return (leq(fs1,fs2) ? fs1 : fs2);
	}
	
	public static FieldSet max(FieldSet fs1, FieldSet fs2) {
		return (leq(fs1,fs2) ? fs2 : fs1);
	}

	/**
	 * Returns all the fields in the fieldset, as jq_Field objects.
	 * 
	 * @return
	 */
	// WARNING: (*) field is not taken into account properly
	public List<jq_Field> getFields() {
		ArrayList<jq_Field> list = new ArrayList<jq_Field>();
		DomAbsField fields = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		int n = val;
		int i = 0;
		while (n>0) {
			if (n % 2 == 1) { list.add(fields.get(i)); }
			n = n>>>1;
			i++;
		}
		return list;
	}

	/**
	 * Returns all the indexes of fields in the fieldset, as integers.
	 * 
	 * @return
	 */
	public List<Integer> getFieldIds() {
		ArrayList<Integer> list = new ArrayList<Integer>();
		int n = val;
		int i = 0;
		while (n>0) {
			if (n % 2 == 1) { list.add(i); }
			n = n>>>1;
			i++;
		}
		return list;
	}

	/**
	 * Returns a list of booleans such that the n-th boolean is true iff the field with id n
	 * is in the fieldset.
	 * 
	 * @return
	 */
	public List<Boolean> getBooleanList() {
		ArrayList<Boolean> list = new ArrayList<Boolean>();
		int n = val;
		while (n>0) {
			list.add(n % 2 == 1);
			n = n>>>1;
		}
		return list;
	}

	/**
	 * Output method.
	 */
	public String toString() {
		String str = "{";
		DomAbsField fields = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		int n = val;
		int i = 0;
		boolean firstField = true;
		while (n>0) {
			if (n % 2 == 1) {
				str = str + (firstField?"":" ") + shortName(fields.get(i));
				firstField = false;
			}
			n = n>>>1;
			i++;
		}
		return str + "}";
	}
	
	/**
	 * Returns a short name for a field, or "*" for the "*" field (which is
	 * actually represented by null). 
	 * @param fld The field to be output.
	 * @return a short String representation.
	 */
	protected static String shortName(jq_Field fld) {
		if (fld == null) return "*"; // the "*" field
		else {
			String str = fld.toString();
			int n = str.indexOf(':');
			if (n>=0) return str.substring(0,n);
			else return str;
		}
	}

	public boolean contains(jq_Field fld) {
		DomAbsField fields = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		int id = fields.indexOf(fld);
		// returns true whenever the field is not tracked explicitly;
		return (((val/(1<<id))%2) == 1 || id <0);
	}

	public boolean containsOnly(jq_Field fld) {
		DomAbsField fields = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		int id = fields.indexOf(fld);
		// returns true whenever the field is not tracked explicitly;
		// TO-DO: check this
		return (val == (1 << id) || (id<0));
	}

	/**
	 * returns all the fieldsets which are a superset of fs1 and a subset of fs2
	 * @param lower
	 * @param upper
	 * @return
	 */
	public static ArrayList<FieldSet> inBetween(FieldSet lower, FieldSet upper) {
		if (leq(lower,upper))
			return inBetween_aux(0,lower,upper);
		else return new ArrayList<FieldSet>();
	}
	
	private static int length() {
		DomAbsField fields = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		return fields.size();
	}

	/**
	 * recursive procedure
	 * 
	 * @param pos
	 * @param length
	 * @param lower
	 * @param upper
	 * @return
	 */
	private static ArrayList<FieldSet> inBetween_aux(int pos,FieldSet lower,FieldSet upper) {
		if (pos == length()) { // job done
			ArrayList<FieldSet> list = new ArrayList<FieldSet>();
			list.add(emptyset());
			return list;
		} else {
			if (lower.bit(pos) == 0 && upper.bit(pos) == 1) {
				ArrayList<FieldSet> fss = inBetween_aux(pos+1,lower,upper);
				ArrayList<FieldSet> list = new ArrayList<FieldSet>();
				for (FieldSet fs : fss) list.add(setBit(fs,pos));
				list.addAll(fss);
				return list;
			} else {
				ArrayList<FieldSet> fss = inBetween_aux(pos+1,lower,upper);
				ArrayList<FieldSet> list = new ArrayList<FieldSet>();
				if (lower.bit(pos) == 1)
					for (FieldSet fs : fss) list.add(setBit(fs,pos));
				else list = fss;
				return list;
			}
		}
	}
	
	/**
	 * pos is counted from the left
	 * 
	 * @param pos
	 * @return
	 */
	private int bit(int pos) {
		return ((val >> (length()-pos-1)) % 2);
	}

	private static FieldSet setBit(FieldSet fs,int pos) {
		DomFieldSet fieldsets = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
		int val_x = fs.val | 1<<(length()-pos-1);
		return fieldsets.get(val_x);		
	}
	
	private static FieldSet unsetBit(FieldSet fs,int pos) {
		DomFieldSet fieldsets = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
		int val_x = fs.val & ~(1<<(length()-pos-1));
		return fieldsets.get(val_x);		
	}

	public ArrayList<FieldSet> getSubsets() {
		return inBetween(emptyset(),this);
	}
	
	
	
}
