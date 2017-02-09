package test;

import java11.io.BufferedInputStream;
import java11.io.StringBufferInputStream;

@SuppressWarnings("deprecation")
public class BufferedInputStreamTest {

	public static void main(String[] args) throws Exception {
		new BufferedInputStreamTest().run();
	}
	
	private void run() throws Exception {
		final BufferedInputStream var1 = new BufferedInputStream(new StringBufferInputStream("<z Tlc"));

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					var1.close();
				} catch (Throwable t) {
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					var1.read();
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
