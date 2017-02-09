package test;

import java.util.ConcurrentModificationException;

import org.jfree.chart.plot.XYPlot;

public class XYPlotTest {

	public static void main(String[] args) {
		new XYPlotTest().run();
	}
	
	private void run() {
		final XYPlot plot = new XYPlot();
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					plot.mapDatasetToDomainAxis(1, 3);
				} catch (Throwable t) {
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					plot.clone();
				} catch (Throwable t) {
					if (t instanceof ConcurrentModificationException) {
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
 