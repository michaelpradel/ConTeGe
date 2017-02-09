package microbenchmark.subclassing;

public class StackOverflowSub extends SuperWithInterface {
	public int computeInt() {
		computeInt();
		return 42;
	}
}
