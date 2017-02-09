package test;

import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;

public class TimeSeriesTest {

	public static void main(String[] args) {
		new TimeSeriesTest().run();
	}
	
	private void run() {
		final TimeSeries series = new TimeSeries("abc");
		final Day day1 = new Day(1,1,1970);
		final Day day2 = new Day(1,1,1971);
		final Day day3 = new Day(1,1,1972);
		final Day day4 = new Day(1,1,1973);

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					series.add(day1, 1.0);
					series.add(day2, 2.0);
				} catch (Throwable t) {
					if (t instanceof NullPointerException) {
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
					series.add(day3, 3.0);
					series.add(day4, 4.0);
				} catch (Throwable t) {
					if (t instanceof NullPointerException) {
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
