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
 * Gestiona la informaci�n que se va calculando para cada m�todo y contexto.
 * La pareja m�todo+contexto la ponemos en un objeto de la clase Entry
 * (inicialmente, entry s�lo contendr� un m�todo; luego iremos incorporando lo
 * otro).
 * 
 * Al principio, al crear un objeto SummaryManager, se construye la lista de
 * entries y no hay ninguna informaci�n sobre cada una de ellas.
 * 
 * Cuando en esta clase hablamos de Entry, lo que queremos decir es "m�todo
 * m�s contexto en el que es llamado"; por ejemplo, si m se llama 2 veces,
 * aqu� en principio aparece dos veces (usamos un an�lisis pre-existente para
 * sacar esta informaci�n).
 * 
 * @author damiano
 *
 */
public class SummaryManager {
	/**
	 * El objeto de tipo Object lo vamos a cambiar por informaci�n m�s 
	 * espec�fica (el Summary) en cuanto lo tengamos hecho
	 */
	ArrayList<Pair<Entry,Object>> summaryList;
	
	/**
	 * Construye una lista de pares (Entry, informaci�n) donde al principio
	 * la informaci�n es null.
	 * 
	 * @param main_method el m�todo principal del analysis (el especificado
	 * en el fichero de input)
	 *
	 */
	public SummaryManager(jq_Method main_method, EntryManager entryManager) {
		summaryList = new ArrayList<Pair<Entry,Object>>();
		
		ArrayList<Entry> entryList = entryManager.getList();
		
		for (Entry e : entryList) {
			summaryList.add(new Pair<Entry,Object>(e,null));
		}
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
	public boolean updateSummary(Entry entry,Object information) {
		
		return false;
	}

}