package test;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java141.util.logging.Logger;

public class LoggerTest {

	public static void main(String[] args) {
		new LoggerTest().run();
	}
	
	private void run() {
		final Logger logger = Logger.getAnonymousLogger();
		final Filter filter1 = logger.getFilter();
		final Filter filter2 = new MyFilter();
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.setFilter(filter1);
				} catch (Throwable t) {
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.setFilter(filter2);
					logger.info("abc");
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

	class MyFilter implements Filter {

		public boolean isLoggable(LogRecord record) {
			return true;
		}
		
	}
	
}
