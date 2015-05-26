package chord.analyses.damianoCyclicity;

import chord.analyses.damianoAnalysis.Utilities;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;

@Chord(name = "cyclicity",
       consumes = { "P", "I", "M", "V", "F", "AbsF", "FSet", "VT", "Register", "UseDef" },
       produces = { }
)
public class Cyclicity extends JavaAnalysis {

    @Override public void run() {
    	Utilities.setVerbose(false);
    	
    	CyclicityFixpoint fp = new CyclicityFixpoint();

    	fp.init();
    	fp.run();
    	    	
    	fp.printOutput();
    }

}
