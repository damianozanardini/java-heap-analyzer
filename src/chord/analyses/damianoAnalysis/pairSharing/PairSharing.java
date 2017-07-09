package chord.analyses.damianoAnalysis.pairSharing;

import chord.analyses.damianoAnalysis.Utilities;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;

@Chord(name = "PairSharing",
       consumes = { "P", "I", "M", "V", "F", "VT", "Register", "Quad", "UseDef" },
       produces = { "PairSharing" }
)
public class PairSharing extends JavaAnalysis {

    @Override public void run() {
    	Utilities.setVerbose(false);
    	
    	PairSharingFixpoint fp = new PairSharingFixpoint();

    	fp.init();
    	fp.run();
    	    	
    	fp.printOutput();
    }

}
