package chord.analyses.jgbHeap;

import java.util.ArrayList;

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
 * Gestiona la información que se va calculando para cada método y contexto.
 * La pareja método+contexto la ponemos en un objeto de la clase Entry
 * (inicialmente, entry sólo contendrá un método; luego iremos incorporando lo
 * otro).
 * 
 * Al principio, al crear un objeto SummaryManager, se construye la lista de
 * entries y no hay ninguna información sobre cada una de ellas.
 * 
 * Cuando en esta clase hablamos de Entry, lo que queremos decir es "método
 * más contexto en el que es llamado"; por ejemplo, si m se llama 2 veces,
 * aquí en principio aparece dos veces (usamos un análisis pre-existente para
 * sacar esta información).
 * 
 * @author damiano
 *
 */
public class SummaryManager {
	
	ArrayList<Pair<Entry,Object>> list;
	
	/**
	 * Construye una lista de pares (Entry, información) donde al principio
	 * la información es null.
	 * 
	 * @param main_method el método principal del analysis (el especificado
	 * en el fichero de input)
	 *
	 */
	public SummaryManager(jq_Method main_method) {
		list = new ArrayList<Pair<Entry,Object>>();
		
		ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCI.load();
		RelView relCIview = relCI.getView();
		PairIterable<Ctxt,Quad> pairs = relCIview.getAry2ValTuples();
		for (Pair<Ctxt,Quad> p: pairs) {
			Quad q = p.val1;
			Operator operator = q.getOperator();
			if (operator instanceof Invoke) {
				list.add(new Pair<Entry,Object>(
						new Entry(Invoke.getMethod(q).getMethod(),p.val0),null));
			}
		}
		// add main method with empty context
		ProgramDom domC = (ProgramDom) ClassicProject.g().getTrgt("C");
		Entry e = new Entry(main_method,(Ctxt) domC.get(0));
		list.add(new Pair<Entry,Object>(e,null));
	}
	
	/**
	 * This method is supposed to update the summary list with new abstract
	 * information. The returned boolean is false iff the new information
	 * is already included in the old one.
	 * 
	 * @param entry
	 * @param information
	 * @return
	 */
	public boolean updateInfo(Entry entry,Object information) {
		
		return false;
	}

}
