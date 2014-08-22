package microbenchmark.deadlock;

public class CUT {

	Object lock1 = new Object();
	Object lock2 = new Object();
	
	public void m1() {
		synchronized (lock1) {
			synchronized (lock2) {
				System.out.println("foo");
			}
		}		
	}
	
	public void m2() {
		synchronized (lock2) {
			synchronized (lock1) {
				System.out.println("bar");
			}
		}		
	}
	
}
