package microbenchmark.subclassing;

public class SeqUnsafeSub extends SuperWithInterface {
	public int computeInt() {
		if (state.size() == 1) throw new IllegalStateException();
		else return 42;
	}
}
