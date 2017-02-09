package test;

import java.io.ByteArrayOutputStream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.SjsxpDriver;

public class XStreamTest {

	public static void main(String[] args) throws Exception {
		new XStreamTest().run();
	}
	
	private void run() throws Exception {
		SjsxpDriver driver = new SjsxpDriver();
		final XStream xstream = new XStream(driver);

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					xstream.createObjectOutputStream(new ByteArrayOutputStream());
				} catch (Throwable t) {
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					xstream.toXML("aaa");
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
