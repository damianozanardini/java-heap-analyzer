package chord.analyses.damianoCycle;

import chord.analyses.damianoAnalysis.Utilities;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;

@Chord(name = "cyclicity",
       consumes = { "P", "I", "M", "V", "F", "AbsF", "FSet", "VT", "Register", "UseDef" }
)
public class Cycle extends JavaAnalysis {

    @Override public void run() {
    	Utilities.setVerbose(false);
    	
    	CycleFixpoint fp = new CycleFixpoint();

    	fp.init();
    	fp.run();
    	    	
    	fp.printOutput();
    }

}
