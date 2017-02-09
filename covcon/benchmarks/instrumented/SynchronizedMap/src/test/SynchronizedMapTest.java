package test;

import java16.util.Collections;
import java16.util.Factory;

public class SynchronizedMapTest {

	public static void main(String[] args) {
		new SynchronizedMapTest().run();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void run() {
		final Collections.SynchronizedMap m1 = Factory.createSyncMap();
		final Collections.SynchronizedMap m2 = Factory.createSyncMap();
		m1.put(m2, 23);
		m2.put(m1, 5);
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					m1.hashCode();
				} catch (Throwable t) {
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					m2.hashCode();
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
