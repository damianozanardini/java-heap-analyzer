package chord.analyses.damianoAnalysis.jgbHeap;

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
import chord.analyses.damianoAnalysis.ParseInputLineException;
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

@Chord(name = "Jheap",
consumes = { "P", "I", "M", "V", "F", "JAbsField", "JFieldSet", "VT", "Register", "C", "CH", "CI", "rootCM", "reachableCM", "JEntry" },
produces = { "JHeapCycle", "JHeapShare" }
		)
public class Heap extends JavaAnalysis {
	
	// The program to analyze
	protected HeapProgram programToAnalyze;
	
	// The program whose information is being read from the input file
	private HeapProgram act_Program;
	
	// The method whose information is being read from the input file
	private jq_Method act_Method;


	/**
	 * 	Get the default method
	 */
	protected jq_Method getMethod() {	
		Utilities.debug("- setMethod: SETTING METHOD TO DEFAULT: main");
		return getMethod(Program.g().getMainMethod());
	}

	/**
	 * Get the method that corresponds to a String or a jq_Method object.
	 * @param o The method.
	 */
	protected jq_Method getMethod(Object o) {
		jq_Method meth = null;
		if(o == null) meth = getMethod();
		if(o instanceof jq_Method){
			Utilities.debug("- setMethod: SETTING METHOD FROM jq_Method OBJECT: " + o);
			meth = (jq_Method) o;
		}else if(o instanceof String){
			Utilities.debug("- setMethod: SETTING METHOD FROM STRING: " + o);
			List<jq_Method> list = new ArrayList<jq_Method>();
			DomM methods = (DomM) ClassicProject.g().getTrgt("M");
			for (int i=0; i<methods.size(); i++) {
				jq_Method m = (jq_Method) methods.get(i);
				if (m!=null) {
					if (m.getName().toString().equals((String) o)) {
						list.add(m);
					}
				}
			}	
			if (list.size()==1) { meth = getMethod(list.get(0)); }
			else { meth = getMethod(); }
		}
		return meth;
	}

	/**
	 * Get the program to be analyzed.
	 * 
	 * @return the method to be analyzed.
	 */
	public HeapProgram getProgram () {
		return this.programToAnalyze;
	}


	/**
	 * HeapMethod process each independent method. 
	 */
	protected HeapMethod hm;
	
	/**
	 * HeapFixpoint process the instructions of each method
	 */
	protected HeapFixpoint fp;

	@Override 
	public void run() {
		Utilities.setVerbose(true);

		// DEBUG
		ControlFlowGraph cfg = CodeCache.getCode(getMethod());
 		new PrintCFG().visitCFG(cfg);
		
		// CREATE LIST OF HEAPPROGRAMS
		programToAnalyze = new HeapProgram(getMethod());
		HeapProgram p = programToAnalyze;
		// READ INPUT FILE
		readInputFile();

		
		Utilities.out("[INICIO] ANALISIS PROGRAMA " + p.getMainMethod());
			
		// NEW HEAPMETHOD
		hm = new HeapMethod();
		
		// WHILE CHANGES
		boolean changed;
		int iteration = 1;
		do{
			changed = false;
			Utilities.out(" /-----/ ITERATION: " + iteration);
			// ANALYSIS EACH METHOD 
			for(Entry e : p.getMethodList()){
				boolean changedprime = false;
				//Utilities.out("- RELS BEFORE ANALYZE METHOD " + e.getMethod());
				//p.getRelShare(e).output();
				//p.getRelCycle(e).output();
					
				// LOAD INPUT INFORMATION AND CHANGE REGISTERS FOR LOCALS
				if(p.getSummaryManager().getSummaryInput(e) != null){
					Utilities.out("- [INIT] PREPARING INPUT OF ENTRY " + e + " ("+e.getMethod()+")");
					changedprime = p.updateRels(e);
					changed |= changedprime;
					if(changedprime) Utilities.out("- [END] PREPARING INPUT OF ENTRY " + e + " WITH CHANGES");
					else Utilities.out("- [END] PREPARING INPUT OF ENTRY " + e + " WITH NO CHANGES"); 
				}
					
				// NEW HEAPFIXPOINT
				fp = new HeapFixpoint(e,p);
				fp.setSummaryManager(p.getSummaryManager());
				fp.setEntryManager(p.getEntryManager());
				hm.setHeapFixPoint(fp);
				
					
				// ANALYSIS METHOD
				changed |= hm.runM(e.getMethod());
					
				// STORE/UPDATE OUTPUT INFORMATION
				// ELIMINAMOS VARIABLES GHOST ANTES DE COPIAR LAS TUPLAS A OUTPUT
				p.deleteGhostVariables(e);
					
				// COPIAMOS LAS TUPLAS A OUTPUT
				AccumulatedTuples acc = fp.getAccumulatedTuples();
				ArrayList<Pair<Register,FieldSet>> cycle = new ArrayList<>();
				ArrayList<chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet>> share = new ArrayList<>();
				List<Register> paramRegisters = new ArrayList<>();
					
				int begin = 0;
				if(e.getMethod().isStatic()){ 
			    	begin = 0;
			    }else{ 
			    	begin = 1; 
			    }
					
				for(int i = begin; i < e.getMethod().getParamWords(); i++){
					if(e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getCallSite().getUsedRegisters().get(i).getType()).isTemp()) continue;
						paramRegisters.add(e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getMethod().getParamTypes()[i]));
					}
					
					Utilities.out("- [INIT] UPDATE OUTPUT INFORMATION OF ENTRY " + e);
					for(Register r : paramRegisters){
			    		for(Register r2 : paramRegisters){
			    			//Utilities.out("----- R: " + r + " --- R: " + r2);
			    			share.addAll(acc.getSFor(e,r, r2));
			    		}
			    		//Utilities.out("----- R: " + r);
			    		cycle.addAll(acc.getCFor(e,r));
			    	}
					
					AbstractValue av = new AbstractValue();
					av.setSComp(new STuples(share));
					av.setCComp(new CTuples(cycle));
					changedprime = p.getSummaryManager().updateSummaryOutput(e, av);
					changed |= changedprime;
					if(changedprime) Utilities.out("- [END] UPDATE OUTPUT INFORMATION OF ENTRY "+ e +" WITH CHANGES");
					else Utilities.out("- [END] UPDATE OUTPUT INFORMATION OF ENTRY " +e+" WITH NO CHANGES");
					//Utilities.out("- RELS AFTER ANALYZE METHOD " + e.getMethod());
					//p.getRelShare().print();
					//p.getRelCycle().print();
					//p.getRelShare().accumulatedTuples.print();
					
				}
				iteration++;
			} while (changed);
			Utilities.out("[FIN] ANALISIS PROGRAMA " + p.getMainMethod());
			p.printOutput();
		//}

		//START PRUEBAS 19/02/2016
 		/*
     	cfg = CodeCache.getCode(an_method);
 		new PrintCFG().visitCFG(cfg);
 		//
 		CSCGAnalysis cg = new CSCGAnalysis();
    	cg.run();
    	ICSCG callgraph = cg.getCallGraph();
    	System.out.println("UNTIL HERE");
		ProgramDom domC = (ProgramDom) ClassicProject.g().getTrgt("C");
		for (int i=0; i<domC.size(); i++) {
			System.out.println("   C: " + domC.get(i));
		}
    	Set<Pair<Ctxt, jq_Method>> nodes = callgraph.getNodes();
    	for (Pair<Ctxt, jq_Method> node : nodes) {
    		System.out.println("   Call-Graph NODE (context,method): (" + node.val0 + ", " + node.val1  + ")");
    	}    	
		ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCI.load();
		RelView relCIview = relCI.getView();
		PairIterable<Object,Object> pairs = relCIview.getAry2ValTuples();
		for (Pair<Object,Object> p: pairs) {
			System.out.println("   CI: " + p.val0 + " --> " + p.val1);
		}
		ProgramRel relrootCM = (ProgramRel) ClassicProject.g().getTrgt("rootCM");
		relrootCM.load();
		RelView relrootCMview = relrootCM.getView();
		PairIterable<Object,Object> rpairs = relrootCMview.getAry2ValTuples();
		for (Pair<Object,Object> p: rpairs) {
			System.out.println("   rootCM: " + p.val0 + " --> " + p.val1);
		}
		ProgramRel relreachableCM = (ProgramRel) ClassicProject.g().getTrgt("reachableCM");
		relreachableCM.load();
		RelView relreachableCMview = relreachableCM.getView();
		PairIterable<Object,Object> rrpairs = relreachableCMview.getAry2ValTuples();
		for (Pair<Object,Object> p: rrpairs) {
			System.out.println("   reachableCM: " + p.val0 + " --> " + p.val1);
		}*/

		// END PRUEBAS 19/02/2016
	}

	/**
	 * 
	 */
	protected void readInputFile() {
		Utilities.out("[INIT] READ FILE");
		try {
			BufferedReader br = new BufferedReader(new FileReader(Config.workDirName + "/input"));
			DomFieldSet DomFieldSet = (DomFieldSet) ClassicProject.g().getTrgt("JFieldSet");
			DomFieldSet.run();
			String line = br.readLine();
			while (line != null) {
				try {
					parseInputLine(line);
				} catch (ParseInputLineException e) {
					Utilities.out("[ERROR] IMPOSSIBLE TO READ LINE: " + e);
				}
				line = br.readLine();
			}
			br = new BufferedReader(new FileReader(Config.workDirName + "/input")); // back to the beginning
			setTrackedFields(br);
			br.close();
		} catch (IOException e) {
			System.out.println("[ERROR] file " + Config.workDirName + "/input" + " not found, assuming");
			System.out.println(" - method to be analyzed: main method");
			System.out.println(" - all fields tracked explicitly");
			System.out.println(" - empty input");
			programToAnalyze = new HeapProgram(getMethod());
			DomFieldSet DomFieldSet = (DomFieldSet) ClassicProject.g().getTrgt("JFieldSet");
			DomFieldSet.fill();
		}
		Utilities.out("[END] READ FILE");
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
	protected void parseInputLine(String line0) throws ParseInputLineException {
		Utilities.out("- [INIT] READ LINE '" + line0 +"'...");
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return; // empty line
		String[] tokens = line.split(" ");
		if (tokens[0].equals("M")) {
			act_Method = getMethod(tokens[1]);
			return;
		}
		if (tokens[0].equals("heap")) {
			if (tokens[1].equals("S")) { // it is a sharing statement
				try {
					// The last method added is which belows the registers
					final Register r1 = RegisterManager.getRegFromInputToken(act_Method,tokens[2]);
					final Register r2 = RegisterManager.getRegFromInputToken(act_Method,tokens[tokens.length-1]);
					boolean barFound = false;
					int i;
					for (i = 3; i < tokens.length-1 && !barFound; i++) {
						if (tokens[i].equals("/")) barFound = true;
					}
					if (!barFound) {
						System.out.println("- [ERROR] separating bar / not found... ");
						throw new ParseInputLineException(line0);
					}
					final FieldSet FieldSet1 = parseFieldsFieldSet(tokens,3,i-1);
					final FieldSet FieldSet2 = parseFieldsFieldSet(tokens,i,tokens.length-1);
					for(Entry e : programToAnalyze.getEntriesMethod(act_Method))
						programToAnalyze.getRelShare().condAdd(e, r1,r2,FieldSet1,FieldSet2);
				} catch (NumberFormatException e) {
					System.out.println("- [ERROR] incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("- [ERROR] illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (ParseFieldException e) {
					if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
						System.out.println("- [ERROR] could not find field " + e.getField());
					if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
						System.out.println("- [ERROR] could not resolve field (multiple choices)" + e.getField());
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("- [ERROR] something went wrong... " + e.getMessage());
					throw new ParseInputLineException(line0);
				}
				Utilities.out("- [END] READ LINE '" + line0 +"' (S)");
				return;
			}
			if (tokens[1].equals("C")) { // it is a cyclicity statement
		
				try {
					final Register r = RegisterManager.getRegFromInputToken(act_Method,tokens[2]);
					final FieldSet FieldSet = parseFieldsFieldSet(tokens,3,tokens.length);
					for(Entry e : programToAnalyze.getEntriesMethod(act_Method))
						programToAnalyze.getRelCycle().condAdd(e, r,FieldSet);
				} catch (NumberFormatException e) {
					System.out.println("- [ERROR] incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("- [ERROR] illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (ParseFieldException e) {
					if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
						System.out.println("- [ERROR] could not find field " + e.getField());
					if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
						System.out.println("- [ERROR] could not resolve field (multiple choices)" + e.getField());
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("- [ERROR] something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
				Utilities.out("- [END] READ LINE '" + line0 +"' (C)");
				return;
			}
			if (tokens[1].equals("S?")) { // it is a sharing statement on output
				try {
					Register r1 = RegisterManager.getRegFromInputToken_end(act_Method,tokens[2]);
					Register r2 = RegisterManager.getRegFromInputToken_end(act_Method,tokens[3]);
					//act_Program.getOutShare(act_Program.getMainEntry()).add(new Pair<Register,Register>(r1,r2));
				} catch (NumberFormatException e) {
					System.out.println("- [ERROR] incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("- [ERROR] illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("- [ERROR] something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
				Utilities.out("- [END] READ LINE '" + line0 +"' (S?)");
				return;
			}
			if (tokens[1].equals("C?")) { // it is a cyclicity statement on output
				try {
					Register r = RegisterManager.getRegFromInputToken_end(act_Method,tokens[2]);
					//act_Program.getOutCycle(act_Program.getMainEntry()).add(r);
				} catch (NumberFormatException e) {
					System.out.println("- [ERROR] incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("- [ERROR] illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("- [ERROR] something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
				Utilities.out("- [END] READ LINE '" + line0 +"' (C?)");
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
	protected FieldSet parseFieldsFieldSet(String[] tokens, int idx1, int idx2) throws ParseFieldException {
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
				DomAbsField absF = (DomAbsField) ClassicProject.g().getTrgt("JAbsField");
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
		if (tokens[0].equals("FS")) { // it is the list of fields to be tracked explicitly
			try {
				List<jq_Field> l = parseFieldsList(tokens,1,tokens.length);
				DomAbsField absF = (DomAbsField) ClassicProject.g().getTrgt("JAbsField");
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
}
