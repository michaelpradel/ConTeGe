package microbenchmark.subclassing;

public class OutputDiffSeqSub extends OutputDiffSeqSuper {

	@Override public int get() {
		return myInt * 2;
	}
	
}
