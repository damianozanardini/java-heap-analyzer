package chord.analyses.jgbHeap;

import joeq.Class.jq_Method;

/**
 * Gestiona la informaci�n que se va calculando para cada m�todo y contexto.
 * Al principio, al crear un objeto SummaryManager, se construye la lista de
 * entries y no hay ninguna informaci�n sobre cada uno de ellos.
 * 
 * Cuando en esta clase hablamos de m�todo, lo que queremos decir es "m�todo
 * m�s contexto en el que es llamado"; por ejemplo, si m se llama 2 veces,
 * aqu� en principio aparece dos veces (usamos un an�lisis pre-existente para
 * sacar esta informaci�n).
 * 
 * @author damiano
 *
 */
public class SummaryManager {
	
	/**
	 * Construye una lista de pares (m�todo, informaci�n) donde al principio
	 * la informaci�n es null.
	 * 
	 * @param main_method el m�todo principal del analysis (el especificado
	 * en el fichero de input)
	 *
	 */
	public SummaryManager(jq_Method main_method) {
		
	}

}
