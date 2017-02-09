package test;

import org.jfree.chart.axis.NumberAxis;

public class NumberAxisTest {

	public static void main(String[] args) throws Exception {
		new NumberAxisTest().run();
	}
	
	private void run() throws Exception {
		final NumberAxis axis = new NumberAxis("SX5H");

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					axis.centerRange(-1.0d);
				} catch (Throwable t) {
					if (t instanceof IllegalArgumentException) {
						System.out.println("\n--------------------\nBug found:\n");
						t.printStackTrace(System.out);
						System.out.println("---------------------\n");
						System.exit(0);	
					}
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					axis.centerRange(-1.0d);
				} catch (Throwable t) {
					if (t instanceof IllegalArgumentException) {
						System.out.println("\n--------------------\nBug found:\n");
						t.printStackTrace(System.out);
						System.out.println("---------------------\n");
						System.exit(0);						
					}
				}
			}
		});

		t1.start();
		t2.start();
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

}
