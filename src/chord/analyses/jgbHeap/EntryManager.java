package chord.analyses.jgbHeap;

import java.util.ArrayList;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.alias.Ctxt;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * El objetivo de esta clase es simplemente generar la lista de objetos Entry
 * que corresponden a los m�todos y su informaci�n de llamada.
 * 
 * @author damiano
 *
 */
public class EntryManager {
	private ArrayList<Entry> entryList;
	
	/**
	 * Construye la lista de objetos Entry y la guarda en entryList
	 */
	public EntryManager(jq_Method main_method) {
		entryList = new ArrayList<Entry>();
		
		ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCI.load();
		RelView relCIview = relCI.getView();
		PairIterable<Ctxt,Quad> pairs = relCIview.getAry2ValTuples();
		for (Pair<Ctxt,Quad> p: pairs) {
			Quad q = p.val1;
			Operator operator = q.getOperator();
			if (operator instanceof Invoke) {
				entryList.add(new Entry(Invoke.getMethod(q).getMethod(),p.val0));
			}
		}
		// add main method with empty context
		ProgramDom domC = (ProgramDom) ClassicProject.g().getTrgt("C");
		Entry e = new Entry(main_method,(Ctxt) domC.get(0));
		entryList.add(e);
	}

	/**
	 * Este m�todo se llama cuando se est� analizando una instrucci�n (Quad) de
	 * llamada a m�todo; dependiendo del sitio de la llamada, se devolver� la
	 * Entry correspondiente al m�todo llamado y a d�nde se llama
	 * 
	 * @param instruction
	 * @return
	 */
	public Entry getRelevantEntry(Quad instruction) {
		
		
		return null;
	}
	
	public ArrayList<Entry> getList() {
		return entryList;
	}
	
	public void printList() {
		System.out.println("*** Printing entry list...");
		for (Entry e : entryList) {
			System.out.println("    " + e.getMethod() + " - " + e.getContext());
		}
		System.out.println("*** Done.");
	}
		
	
	
	
	
	
}