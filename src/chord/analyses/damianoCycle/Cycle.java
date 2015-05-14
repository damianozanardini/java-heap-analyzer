package chord.analyses.damianoCycle;

import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;

@Chord(name = "cycle",
       consumes = { "P", "I", "M", "V", "F", "AbsF", "FSet", "VT", "Register", "UseDef" },
       produces = { "Reach" }
)
public class Cycle extends JavaAnalysis {

    @Override public void run() {
    	Utilities.verbose = false;
    	
    	CycleFixpoint fp = new CycleFixpoint();

    	fp.init();
    	fp.run();
    	    	
    	fp.printOutput();
    }

}
