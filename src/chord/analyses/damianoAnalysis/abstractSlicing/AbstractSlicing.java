package chord.analyses.damianoAbstractSlicing;

import joeq.Class.jq_Method;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.damianoCyclicity.CyclicityFixpoint;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;
import chord.util.tuple.object.Pair;

@Chord(name = "a-slicing",
       consumes = { "P", "I", "M", "V", "F", "AbsF", "VT", "Register", "UseDef", "PairSharing" },
       produces = { }
)
public class AbstractSlicing extends JavaAnalysis {

    @Override public void run() {
    	// Execution of the cyclicity analysis in order to get sharing information
    	//runCyclicity();
    	
    	Utilities.setVerbose(true);
    	ASlicingFixpoint fp = new ASlicingFixpoint();
    	fp.init();
    	Pair<jq_Method,AgreementList> p = fp.getAgreementList();
    	
    	Slicer s = new Slicer(p.val0);
    	s.run(p.val1);
    	
    	// fp.printOutput();
    }

    private void runCyclicity() {
    	// TODO to directly call another analysis is NOT the proper way to do;
    	// the Chord way of modeling analysis interactions should be used instead
    	Utilities.setVerbose(false);
    	CyclicityFixpoint cfp = new CyclicityFixpoint();
    	cfp.init();
    	cfp.run();
    	cfp.save();
    }
    
}
