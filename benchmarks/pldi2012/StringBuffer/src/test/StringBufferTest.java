package test;

public class StringBufferTest {

	public static void main(String[] args) {
		new StringBufferTest().run();
	}
	
	private void run() {
		final StringBuffer sb = new StringBuffer("abc");
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					sb.insert(1, sb);
				} catch (Throwable t) {
					if (t instanceof IndexOutOfBoundsException) {
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
					sb.deleteCharAt(0);
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
