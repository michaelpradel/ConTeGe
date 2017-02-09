package microbenchmark.statechanger;

public class CUT {

	private int state = 0;
	
	private Object lock1 = new Object();
	private Object lock2 = new Object();
	
	public void increment() {
		state++;
	}
	
	public void concBug1() {
		if (state < 3) return;
		
		synchronized (lock1) {
			synchronized (lock2) {
				System.out.println("blubb");
			}
		}
	}
	
	public void concBug2() {
		if (state < 3) return;
		
		synchronized (lock2) {
			synchronized (lock1) {
				System.out.println("bla");
			}
		}
	}
	
}
