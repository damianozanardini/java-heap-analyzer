package chord.analyses.damianoAbstractSlicing;

import joeq.Class.jq_Method;
import chord.analyses.damianoAnalysis.Utilities;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;
import chord.util.tuple.object.Pair;

@Chord(name = "a-slicing",
       consumes = { "P", "I", "M", "V", "F", "AbsF", "VT", "Register", "UseDef", "Share" },
       produces = { }
)
public class AbstractSlicing extends JavaAnalysis {

    @Override public void run() {
    	Utilities.setVerbose(true);
    	
    	ASlicingFixpoint fp = new ASlicingFixpoint();
    	fp.init();
    	Pair<jq_Method,AgreementList> p = fp.getAgreementList();
    	
    	Slicer s = new Slicer(p.val0);
    	s.run(p.val1);
    	
    	// fp.printOutput();
    }

}
