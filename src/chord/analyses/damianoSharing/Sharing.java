package chord.analyses.damianoSharing;

import chord.analyses.damianoAnalysis.Utilities;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;

@Chord(name = "pairSharing",
       consumes = { "P", "I", "M", "V", "F", "VT", "Register", "UseDef" },
       produces = { "PairShare" }
)
public class Sharing extends JavaAnalysis {

    @Override public void run() {
    	Utilities.setVerbose(false);
    	
    	SharingFixpoint fp = new SharingFixpoint();

    	fp.init();
    	fp.run();
    	    	
    	fp.printOutput();
    }

}
