package test;

import java.util.ConcurrentModificationException;

import org.jfree.data.TimeSeriesTableModel;
import org.jfree.data.XYSeries;

public class XYSeriesTest {

	public static void main(String[] args) {
		new XYSeriesTest().run();
	}
	
	private void run() {
		final XYSeries series = new XYSeries("ABC");
		final TimeSeriesTableModel model1 = new TimeSeriesTableModel();
		final TimeSeriesTableModel model2 = new TimeSeriesTableModel();
		final TimeSeriesTableModel model3 = new TimeSeriesTableModel();
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					series.addChangeListener(model1);
					series.addChangeListener(model2);
					series.addChangeListener(model3);
				} catch (Throwable t) {
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					series.fireSeriesChanged();
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

