package chord.analyses.jgbHeap;

import joeq.Class.jq_Method;

/**
 * Gestiona la información que se va calculando para cada método y contexto.
 * Al principio, al crear un objeto SummaryManager, se construye la lista de
 * entries y no hay ninguna información sobre cada uno de ellos.
 * 
 * Cuando en esta clase hablamos de método, lo que queremos decir es "método
 * más contexto en el que es llamado"; por ejemplo, si m se llama 2 veces,
 * aquí en principio aparece dos veces (usamos un análisis pre-existente para
 * sacar esta información).
 * 
 * @author damiano
 *
 */
public class SummaryManager {
	
	/**
	 * Construye una lista de pares (método, información) donde al principio
	 * la información es null.
	 * 
	 * @param main_method el método principal del analysis (el especificado
	 * en el fichero de input)
	 *
	 */
	public SummaryManager(jq_Method main_method) {
		
	}

}
