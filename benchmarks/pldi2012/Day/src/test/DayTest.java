package test;

import org.jfree.data.time.Day;

public class DayTest {

	public static void main(String[] args) {
		new DayTest().run();
	}
	
	private void run() {
		Day.parseDay("r");
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Day.parseDay("J");
					Day.parseDay("K");
				} catch (Throwable t) {
					if (t instanceof NumberFormatException) {
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
					Day.parseDay("1");
					Day.parseDay(")]Fo4");
				} catch (Throwable t) {
					if (t instanceof NumberFormatException) {
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
