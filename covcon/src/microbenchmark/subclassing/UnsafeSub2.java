package microbenchmark.subclassing;

public class UnsafeSub2 extends SuperWithInterface {
	
	public int computeInt() {
		int sum = 0;
		for (Integer i : state) {
			sum += i * 2;
		}
		return sum;
	}
}
