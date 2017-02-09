package microbenchmark.subclassing;

public class OutputDiffConcSub extends OutputDiffConcSuper {

	// same as in super class but without synchronization
	@Override public int incrAndReturn() {
		int tmp = myInt;
		tmp = tmp + 1;
		myInt = tmp;
		return myInt;
	}
	
}
