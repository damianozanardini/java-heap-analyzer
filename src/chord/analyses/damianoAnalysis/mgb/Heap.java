package chord.analyses.damianoAnalysis.mgb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.PrintCFG;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CSCG;
import chord.analyses.alias.CSCGAnalysis;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.ICSCG;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.ParseInputLineException;
import chord.analyses.damianoAnalysis.ProgramPoint;
import chord.analyses.damianoAnalysis.QuadQueue;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.field.DomF;
import chord.analyses.method.DomM;
import chord.bddbddb.Rel;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

@Chord(name = "heap",
consumes = { "P", "I", "M", "V", "F", "AbsField", "FieldSet", "VT", "Register", "C", "CH", "CI", "rootCM", "reachableCM", "Entry", "ProgramPoint" }		)
public class Heap extends JavaAnalysis {
			
	// The method whose information is being read from the input file
	private jq_Method initialMethod;

	/**
	 * 	Get the default method
	 */
	private void setInitialMethod() {
		Utilities.begin("SETTING ENTRY METHOD TO DEFAULT: main");
		initialMethod = Program.g().getMainMethod();
		Utilities.end("SETTING ENTRY METHOD TO DEFAULT: main");
	}

	private void setInitialMethod(String str) {
		Utilities.begin("SETTING ENTRY METHOD FROM STRING: " + str);
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
		if (list.size()==1) {
			initialMethod = list.get(0);
			Utilities.end("SETTING ENTRY METHOD FROM STRING: " + str + "... SUCCESS");
		}
		else {
			Utilities.end("SETTING ENTRY METHOD FROM STRING: " + str + "... FAILURE");
			setInitialMethod();
		}
	}
		
	@Override 
	public void run() {
		// enables debug messages in the log.txt file
		Utilities.setVerbose(true);
		Utilities.debug("\n\n\n\n----------------------------------------------------------------------------------------");
		Utilities.debug("[BEGIN] PROGRAM ANALYSIS");
		
		// reads the "input" file of the example, and gets the info from there
		readInputFile();

		// gets the code and prints the Control Flow Graph of all methods
		// appearing in some entry
		// (i.e., the Java main method and every method that is called somewhere)
		if (Utilities.isVerbose()) { Utilities.printCFGs(); }
		
		HeapEntry he;
		
		boolean globallyChanged;
		int iteration = 1;
		do {
			Utilities.begin("PROGRAM-LEVEL ITERATION #" + iteration);
			globallyChanged = false;
			
			// analyze each entry (WARNING: this is not optimized: we could analyze only
			// methods whose input information has changed in the previous iteration)
			for (Entry e : GlobalInfo.entryManager.getList()) {
				he = new HeapEntry(e);
				globallyChanged |= he.run();
			}
			Utilities.end("PROGRAM-LEVEL ITERATION #" + iteration);
			iteration++;
		} while (globallyChanged);
		
		Utilities.end("PROGRAM ANALYSIS");
	}

	/**
	 * 
	 */
	private void readInputFile() {
		Utilities.begin("READ INPUT FILE");
		try {
			BufferedReader br = new BufferedReader(new FileReader(Config.workDirName + "/input"));
			String line = br.readLine();
			// the first line should contain the M information (method to analyze)
			boolean	b = false;
			b = parseInitialMethodLine(line);
			if (!b) setInitialMethod();
			// now that the entry method is set, the GlobalInfo can be initialized
			GlobalInfo.init(initialMethod);
			ArrayList<Entry> initialEntries = GlobalInfo.entryManager.getEntriesFromMethod(initialMethod);
			ArrayList<ProgramPoint> initialPPs = new ArrayList<ProgramPoint>();
			for (Entry e : initialEntries) initialPPs.add(GlobalInfo.getInitialPP(e));
			// if the first line did not contain a proper "M" line, then it is re-read
			if (!b) try {
				parseInputLine(line,initialPPs);
			} catch (ParseInputLineException e) {
				Utilities.err("IMPOSSIBLE TO READ LINE: " + e);
			}
			line = br.readLine();
			while (line != null) {
				try {
					parseInputLine(line,initialPPs);
				} catch (ParseInputLineException e) {
					Utilities.err("IMPOSSIBLE TO READ LINE: " + e);
				}
				line = br.readLine();
			}
			br = new BufferedReader(new FileReader(Config.workDirName + "/input")); // back to the beginning
			setTrackedFields(br);
			br.close();
		} catch (IOException e) {
			Utilities.warn("FILE " + Config.workDirName + "/input" + " NOT FOUND, assuming");
			Utilities.warn(" - method to be analyzed: main method");
			Utilities.warn(" - all fields tracked explicitly");
			Utilities.warn(" - empty input");
			setInitialMethod();
			// now that the entry method is set, the GlobalInfo can be initialized
			GlobalInfo.init(initialMethod);
			DomFieldSet DomFieldSet = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
			DomFieldSet.fill();
		}
		Utilities.end("READ INPUT FILE");
	}

	private boolean parseInitialMethodLine(String line0) {
		Utilities.begin("READ M LINE '" + line0 + "'");
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return false; // empty line
		String[] tokens = line.split(" ");
		if (tokens[0].equals("M")) {
			setInitialMethod(tokens[1]);
			Utilities.end("READ M LINE '" + line0 + "': SUCCESS");
			return true;
		} else {
			Utilities.warn("METHOD SPECIFICATION NOT FOUND IN '" + line0 + "'");
			Utilities.end("READ M LINE '" + line0 + "': FAILURE");
			return false;
		}
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
	private void parseInputLine(String line0,ArrayList<ProgramPoint> initialPPs) throws ParseInputLineException {
		Utilities.begin("READ LINE '" + line0 + "'");
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return; // empty line
		String[] tokens = line.split(" ");
		if (tokens[0].equals("heap")) {
			if (tokens[1].equals("S")) { // it is a sharing statement
				final Register r1, r2;
				try {
					r1 = RegisterManager.getRegFromInputToken(initialMethod,tokens[2]);
					r2 = RegisterManager.getRegFromInputToken(initialMethod,tokens[tokens.length-1]);
					// The last method added is which belows the registers
					boolean barFound = false;
					int i;
					for (i = 3; i < tokens.length-1 && !barFound; i++) {
						if (tokens[i].equals("/")) barFound = true;
					}
					if (!barFound) {
						Utilities.err("SEPARATING BAR'/' NOT FOUND");
						throw new ParseInputLineException(line0);
					}
					final FieldSet fs1 = parseFieldsFieldSet(tokens,3,i-1);
					final FieldSet fs2 = parseFieldsFieldSet(tokens,i,tokens.length-1);
					for(Entry e : GlobalInfo.entryManager.getEntriesFromMethod(initialMethod))
						for (ProgramPoint pp : initialPPs)
							GlobalInfo.getAV(pp).getSComp().addTuple(r1, r2, fs1, fs2);							
				} catch (NumberFormatException e) {
					Utilities.err("INCORRECT REGISTER REPRESENTATION: " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					Utilities.err("ILLEGAL REGISTER: " + e);
					throw new ParseInputLineException(line0);
				} catch (ParseFieldException e) {
					if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
						Utilities.err("COULD NOT FIND FIELD " + e.getField());
					if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
						Utilities.err("COULD NOT RESOLVE FIELD (multiple choices)" + e.getField());
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					Utilities.err("SOMETHING WENT WRONG: " + e.getMessage());
					throw new ParseInputLineException(line0);
				}
				Utilities.end("READ LINE '" + line0 + "' (S detected on registers " + r1 + " (" + tokens[2] + ") and " + r2 + " (" + tokens[tokens.length-1] + "))");
				return;
			}
			if (tokens[1].equals("C")) { // it is a cyclicity statement
				final Register r;		
				try {
					r = RegisterManager.getRegFromInputToken(initialMethod,tokens[2]);
					final FieldSet fs = parseFieldsFieldSet(tokens,3,tokens.length);
					for(Entry e : GlobalInfo.entryManager.getEntriesFromMethod(initialMethod))
						for (ProgramPoint pp : initialPPs)
							GlobalInfo.getAV(pp).getCComp().addTuple(r,fs);							
				} catch (NumberFormatException e) {
					Utilities.err("INCORRECT REGISTER REPRESENTATION: " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					Utilities.err("ILLEGAL REGISTER: " + e);
					throw new ParseInputLineException(line0);
				} catch (ParseFieldException e) {
					if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
						Utilities.err("COULD NOT FIND FIELD " + e.getField());
					if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
						Utilities.err("COULD NOT RESOLVE FIELD (multiple choices)" + e.getField());
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					Utilities.err("SOMETHING WENT WRONG: " + e.getMessage());
					throw new ParseInputLineException(line0);
				}
				Utilities.end("READ LINE '" + line0 + "' (C detected on register " + r + " (" + tokens[2] + "))");
				return;
			}
			if (tokens[1].equals("S?")) { // it is a sharing statement on output
				final Register r1, r2;
				try {
					r1 = RegisterManager.getRegFromInputToken_end(initialMethod,tokens[2]);
					r2 = RegisterManager.getRegFromInputToken_end(initialMethod,tokens[3]);
					//act_Program.getOutShare(act_Program.getMainEntry()).add(new Pair<Register,Register>(r1,r2));
				} catch (NumberFormatException e) {
					Utilities.err("INCORRECT REGISTER REPRESENTATION: " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					Utilities.err("ILLEGAL REGISTER: " + e);
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					Utilities.err("SOMETHING WENT WRONG: " + e.getMessage());
					throw new ParseInputLineException(line0);
				}
				Utilities.end("READ LINE '" + line0 + "' (S? detected on registers " + r1 + " (" + tokens[2] + ") and " + r2 + " (" + tokens[3] + "))");
				
				return;
			}
			if (tokens[1].equals("C?")) { // it is a cyclicity statement on output
				final Register r;
				try {
					r = RegisterManager.getRegFromInputToken_end(initialMethod,tokens[2]);
					//act_Program.getOutCycle(act_Program.getMainEntry()).add(r);
				} catch (NumberFormatException e) {
					Utilities.err("INCORRECT REGISTER REPRESENTATION: " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					Utilities.err("ILLEGAL REGISTER: " + e);
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					Utilities.err("SOMETHING WENT WRONG: " + e.getMessage());
					throw new ParseInputLineException(line0);
				}
				Utilities.end("READ LINE '" + line0 + "' (C? detected on register " + r + " (" + tokens[2] + "))");
				return;
			}
		}
	}

	/**
	 * Scans an array {@code tokens} of {@code String} tokens from index {@code idx1}
	 * to {@code idx2}; for each of them, retrieves a field, and put them all together
	 * is an {@code FieldSet} object.
	 *  
	 * @param tokens The array of String.
	 * @param idx1 The index of the first relevant token. 
	 * @param idx2 The index (plus 1) of the last relevant token. 
	 * @return The {@code FieldSet} object representing all parsed fields. 
	 * @throws ParseFieldException if some field name cannot be parsed.
	 */
	private FieldSet parseFieldsFieldSet(String[] tokens, int idx1, int idx2) throws ParseFieldException {
		FieldSet fieldSet = FieldSet.emptyset();
		for (int i=idx1; i<idx2; i++) {
			try {
				jq_Field f = parseField(tokens[i]);
				fieldSet = FieldSet.addField(fieldSet,f);
			} catch (ParseFieldException e) {
				throw e;
			}
		}
		return fieldSet;
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
	private jq_Field parseField(String str) throws ParseFieldException {
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

	private void setTrackedFields(BufferedReader br) {
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
				DomAbsField absF = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
				absF.run();
			}
			br.close();
		} catch (IOException e) {}
	}

	private Boolean parseFLine(String line0) throws ParseInputLineException {
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return false; // empty line
		String[] tokens = line.split(" ");
		if (tokens[0].equals("FS")) { // it is the list of fields to be tracked explicitly
			try {
				List<jq_Field> l = parseFieldsList(tokens,1,tokens.length);
				DomAbsField absF = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
				absF.trackedFields = l;
				absF.run();
				Utilities.debug("EXPLICITLY TRACKING FIELDS " + l);
			} catch (ParseFieldException e) {
				if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
					Utilities.err("COULD NOT FIND FIELD " + e.getField());
				if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
					Utilities.err("COULD NOT RESOLVE FIELD (multiple choices)" + e.getField());
				throw new ParseInputLineException(line0);
			}
			return true;
		}
		return false;
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
	private List<jq_Field> parseFieldsList(String[] tokens, int idx1, int idx2) throws ParseFieldException {
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
}
