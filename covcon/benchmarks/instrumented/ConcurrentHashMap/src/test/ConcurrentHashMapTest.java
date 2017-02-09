package test;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapTest {

	public static void main(String[] args) {
		new ConcurrentHashMapTest().run();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void run() {
		final ConcurrentHashMap map = new ConcurrentHashMap();
		map.put("a", map);

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					map.clear();
					map.hashCode();
				} catch (Throwable t) {
					if (t instanceof StackOverflowError) {
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
					map.putAll(map);
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
