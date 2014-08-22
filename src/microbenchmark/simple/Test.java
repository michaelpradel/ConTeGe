package microbenchmark.simple;


public class Test {

	public static void main(String[] args) {
		for (int i = 0; i < 1000; i++) {
			new Test().run();
		}
	}
	
	private void run() {
		final CUT c = new CUT();
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				c.toggle();
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				c.testAndUse();
			}
		});
		
		t1.start();
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}

