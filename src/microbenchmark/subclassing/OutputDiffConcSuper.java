package microbenchmark.subclassing;

public class OutputDiffConcSuper {

	protected int myInt = 0;
	
	public synchronized int incrAndReturn() {
		int tmp = myInt;
		tmp = tmp + 1;
		myInt = tmp;
		return myInt;
	}
	
}
