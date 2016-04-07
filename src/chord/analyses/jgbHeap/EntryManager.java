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
 * que corresponden a los métodos y su información de llamada.
 * 
 * @author damiano
 *
 */
public class EntryManager {
	private ArrayList<Entry> entryList;
	
	/**
	 * TO-DO: Obtener solo los metodos que se utilizan
	 */
	/**
	 * Construye la lista de objetos Entry y la guarda en entryList
	 */
	
	public EntryManager(jq_Method main_method) {
		entryList = new ArrayList<Entry>();
		
		// add main method with empty context
		ProgramDom domC = (ProgramDom) ClassicProject.g().getTrgt("C");
		// TO-DO: put the entry instruction instead of null
		Entry e = new Entry(main_method,(Ctxt) domC.get(0),null);
		entryList.add(e);

		ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCI.load();
		RelView relCIview = relCI.getView();
		PairIterable<Ctxt,Quad> pairs = relCIview.getAry2ValTuples();
		for (Pair<Ctxt,Quad> p: pairs) {
			Quad q = p.val1;
			Operator operator = q.getOperator();
			if (operator instanceof Invoke) {
				Entry e1 = new Entry(Invoke.getMethod(q).getMethod(),p.val0,q);
				entryList.add(e1);
			}
		}
	}

	/**
	 * Este método se llama cuando se está analizando una instrucción (Quad) de
	 * llamada a método; dependiendo del sitio de la llamada, se devolverá la
	 * Entry correspondiente al método llamado y a dónde se llama
	 * 
	 * @param instruction
	 * @return
	 */
	public Entry getRelevantEntry(Quad instruction) throws NoEntryException {
		for (Entry e : entryList) if (e.getCallSite() == instruction) return e;
		throw new NoEntryException("Entry for instruction " + instruction + " could not be found");
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
