package microbenchmark.subclassing;

public class SafeSub extends SuperWithInterface {

	public int computeInt() {
		int sum = 0;
		synchronized (this) {
			for (Integer i : state) {
				sum += i * 3;
			}	
		}
		return sum;
	}
	
}
