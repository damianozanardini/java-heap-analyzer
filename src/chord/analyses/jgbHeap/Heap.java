package chord.analyses.jgbHeap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.PrintCFG;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CSCG;
import chord.analyses.alias.CSCGAnalysis;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.ICSCG;
import chord.analyses.damianoAnalysis.ParseInputLineException;
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
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

@Chord(name = "heap",
       consumes = { "P", "I", "M", "V", "F", "AbsField", "FieldSet", "VT", "Register", "UseDef", "C", "CH", "CI", "reachableCM" },
       produces = { }
)
public class Heap extends JavaAnalysis {
	
	/**
	 * The method to be analyzed (currently, the analysis is intraprocedural).
	 */
	protected jq_Method an_method;
	
	/**
	 * The methods to be analyzed.
	 */
	protected ArrayList<jq_Method> methods;	

	
	/**
	 * Sets the method to be analyzed (default is main).
	 */
	protected void setMethod() {	
		Utilities.debug("    setMethod: SETTING METHOD TO DEFAULT: main");
		setMethod(Program.g().getMainMethod());
	}
			
	/**
	 * Sets the method to be analyzed (default is main).
	 * @param m The method to be analyzed.
	 */
	protected void setMethod(Object o) {
		
		if(o instanceof jq_Method){
			Utilities.debug("    setMethod: SETTING METHOD FROM jq_Method OBJECT: " + o);
			if (o == null) an_method = Program.g().getMainMethod();
			else an_method = (jq_Method) o;
			methods.add(an_method);
			Utilities.debug("    setMethod: METHOD FINALLY SET TO " + an_method);
		}else if(o instanceof String){
			Utilities.debug("    setMethod: SETTING METHOD FROM STRING: " + o);
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
			if (list.size()==1) setMethod(list.get(0));
			else setMethod();
		}
	}
	
	
	/**
	 * Sets an array of methods to be analyzed. 
	 * @param methods
	 */
	public void setMethods(ArrayList<jq_Method> methods){ this.methods = methods; }
		
	/**
	 * Gets the method to be analyzed.
	 * 
	 * @return the method to be analyzed.
	 */
	public jq_Method getMethod () {
		return an_method;
	}
	
	/**
	 * Gets the methods to be analyzed.
	 * 
	 * @return the method to be analyzed.
	 */
	public ArrayList<jq_Method> getMethods () {
		return this.methods;
	}
	
	protected HeapMethod hm;

    @Override public void run() {
    	Utilities.setVerbose(true);
    	
    	 methods = new ArrayList<>(); 
    	 hm = new HeapMethod();
    	 hm.init();
    	 readInputFile();
    	 for(jq_Method m : methods)
    	 	hm.run(m);
    	 hm.printOutput();
    	 
    	
    	/*HeapFixpoint fp = new HeapFixpoint();
    	fp.init();
    	fp.run();
      	fp.printOutput();*/

    	// PRUEBAS 19/02/2016
    	CSCGAnalysis cg = new CSCGAnalysis();
    	cg.run();
    	ICSCG callgraph = cg.getCallGraph();
    	System.out.println("UNTIL HERE");
    	Set<Pair<Ctxt, jq_Method>> nodes = callgraph.getNodes();
    	for (Pair<Ctxt, jq_Method> node : nodes) {
    		System.out.println("   CG NODE: " + node.val0 + " --> " + node.val1);
    	}
    	//ControlFlowGraph cfg = CodeCache.getCode(Program.g().getMainMethod());
		//new PrintCFG().visitCFG(cfg);
		ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCI.load();
		RelView relCIview = relCI.getView();
		PairIterable<Object,Object> pairs = relCIview.getAry2ValTuples();
		for (Pair<Object,Object> p: pairs) {
			System.out.println("   CI: " + p.val0 + " --> " + p.val1);
		}
    }
    
    protected void readInputFile() {
		Utilities.out("READING FROM INPUT FILE...");
		try {
			BufferedReader br = new BufferedReader(new FileReader(Config.workDirName + "/input"));
			DomFieldSet DomFieldSet = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
			DomFieldSet.run();
			String line = br.readLine();
			while (line != null) {
				try {
					parseInputLine(line);
				} catch (ParseInputLineException e) {
					Utilities.out("IMPOSSIBLE TO READ LINE: " + e);
				}
				line = br.readLine();
			}
			br = new BufferedReader(new FileReader(Config.workDirName + "/input")); // back to the beginning
			setTrackedFields(br);
			br.close();
		} catch (IOException e) {
			System.out.println("ERROR: file " + Config.workDirName + "/input" + " not found, assuming");
			System.out.println(" - method to be analyzed: main method");
			System.out.println(" - all fields tracked explicitly");
			System.out.println(" - empty input");
			setMethod();
			DomFieldSet DomFieldSet = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
			DomFieldSet.fill();
		}
		Utilities.out("READING FROM INPUT FILE DONE");
	}
    
    protected void parseInputLine(String line0) throws ParseInputLineException {
		Utilities.out("  READING LINE '" + line0 +"'...");
		String line;
		if (line0.indexOf('%') >= 0) {
			line = line0.substring(0,line0.indexOf('%')).trim();
		} else line = line0.trim();
		if (line.length() == 0) return; // empty line
		String[] tokens = line.split(" ");
		if (tokens[0].equals("M")) {
			setMethod(tokens[1]);
			return;
		}
		if (tokens[0].equals("heap")) {
			if (tokens[1].equals("S")) { // it is a sharing statement
				try {
					Register r1 = RegisterManager.getRegFromInputToken(getMethod(),tokens[2]);
					Register r2 = RegisterManager.getRegFromInputToken(getMethod(),tokens[tokens.length-1]);
					boolean barFound = false;
					int i;
					for (i = 3; i < tokens.length-1 && !barFound; i++) {
						if (tokens[i].equals("/")) barFound = true;
					}
					if (!barFound) {
						System.out.println("    ERROR: separating bar / not found... ");
						throw new ParseInputLineException(line0);
					}
					FieldSet FieldSet1 = parseFieldsFieldSet(tokens,3,i-1);
					FieldSet FieldSet2 = parseFieldsFieldSet(tokens,i,tokens.length-1);
					hm.getRelShare().condAdd(r1,r2,FieldSet1,FieldSet2);
				} catch (NumberFormatException e) {
					System.out.println("    ERROR: incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("    ERROR: illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (ParseFieldException e) {
					if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
						System.out.println("    ERROR: could not find field " + e.getField());
					if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
						System.out.println("    ERROR: could not resolve field (multiple choices)" + e.getField());
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("    ERROR: something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
				return;
			}
			if (tokens[1].equals("C")) { // it is a cyclicity statement
				try {
					Register r = RegisterManager.getRegFromInputToken(getMethod(),tokens[2]);
					FieldSet FieldSet = parseFieldsFieldSet(tokens,3,tokens.length);
					hm.getRelCycle().condAdd(r,FieldSet);
				} catch (NumberFormatException e) {
					System.out.println("    ERROR: incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("    ERROR: illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (ParseFieldException e) {
					if (e.getCode() == ParseFieldException.FIELDNOTFOUND)
						System.out.println("    ERROR: could not find field " + e.getField());
					if (e.getCode() == ParseFieldException.MULTIPLEFIELDS)
						System.out.println("    ERROR: could not resolve field (multiple choices)" + e.getField());
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("    ERROR: something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
				return;
			}
			if (tokens[1].equals("S?")) { // it is a sharing statement on output
				try {
					Register r1 = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[2]);
					Register r2 = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[3]);
					hm.getOutShare().add(new Pair<Register,Register>(r1,r2));
				} catch (NumberFormatException e) {
					System.out.println("    ERROR: incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("    ERROR: illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("    ERROR: something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
			}
			if (tokens[1].equals("C?")) { // it is a cyclicity statement on output
				try {
					Register r = RegisterManager.getRegFromInputToken_end(getMethod(),tokens[2]);
					hm.getOutCycle().add(r);
				} catch (NumberFormatException e) {
					System.out.println("    ERROR: incorrect register representation " + e);
					throw new ParseInputLineException(line0);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("    ERROR: illegal register " + e);
					throw new ParseInputLineException(line0);
				} catch (RuntimeException e) {
					System.out.println("    ERROR: something went wrong... " + e);
					throw new ParseInputLineException(line0);
				}
				return;
			}
		}
	}
	
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
				DomAbsField absF = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
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
				DomAbsField absF = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
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
