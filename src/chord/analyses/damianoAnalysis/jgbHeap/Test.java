package chord.analyses.damianoAnalysis.jgbHeap;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;

@Chord(name = "Jtest",
	consumes = { "Jheap" },
	produces = { }
)
public class Test extends JavaAnalysis{

	@Override
	public void run(){
		
		Heap heap = (Heap) ClassicProject.g().getTrgt("Jheap");
		if(heap != null){
			System.out.println(heap.getProgram().getMainMethod());
		}
	}

}
