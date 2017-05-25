package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import chord.analyses.alias.Ctxt;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Compiler.Quad.Quad;

/**
 * Stores summaries for each entry.
 * Initially it contains no data, and is filled during the analysis.
 * 
 * @author damiano
 */
public class SummaryManager {
	HashMap<Entry,Summary> summaryList;
	
	/**
	 * Construye una lista de pares (Entry, informaci—n) donde al principio
	 * la informaciï¿½n es null.
	 * 
	 * @param main_method el mï¿½todo principal del analysis (el especificado
	 * en el fichero de input)
	 *
	 */
	public SummaryManager() {
		summaryList = new HashMap<Entry,Summary>();
	}
	
	/**
	 * Update the summary list with new abstract information.
	 * If there is no information for the entry, it is created.
	 * Otherwise, the new info is merged with the old one.
	 * The returned boolean is true iff the new information
	 * is not already included in the old one.
	 * 
	 * @param entry
	 * @param information
	 * @return
	 */
	public boolean updateSummaryInput(Entry entry,AbstractValue a) {
		if (summaryList.containsKey(entry)) {
			Summary s = summaryList.get(entry);
			return s.updateInput(a);
		} else {
			Summary s = new Summary(a,null);
			summaryList.put(entry,s);
			return true;
		}
	}

	public boolean updateSummaryOutput(Entry entry,AbstractValue a) {
		if (summaryList.containsKey(entry)) {
			Summary s = summaryList.get(entry);
			return s.updateOutput(a);
		} else {
			Summary s = new Summary(null,a);
			summaryList.put(entry,s);
			return true;
		}
	}
	
	public AbstractValue getSummaryInput(Entry entry){
		Summary s = summaryList.get(entry);
		if (s != null) {
			return s.getInput();
		} else {
			return null;
		}
	}

	public AbstractValue getSummaryOutput(Entry entry){
		Summary s = summaryList.get(entry);
		if (s != null) {
			return s.getOutput();
		} else {
			return null;
		}
	}
	
}
