package test;

import org.jfree.chart.axis.PeriodAxis;

public class PeriodAxisTest {

	public static void main(String[] args) {
		new PeriodAxisTest().run();
	}
	
	private void run() {
		final PeriodAxis axis = new PeriodAxis("Y");
		final double ub = axis.getUpperBound();
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					axis.getRange();
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
					axis.setUpperBound(ub);
				} catch (Throwable t) {
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
