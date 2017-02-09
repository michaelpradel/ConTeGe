package test;

import java.util.ConcurrentModificationException;

import javax.sql.ConnectionPoolDataSource;

import org.apache.commons.dbcp.datasources.PerUserPoolDataSource;

public class PerUserPoolDataSourceTest {

	public static void main(String[] args) {
		new PerUserPoolDataSourceTest().run();
	}
	
	private void run() {
		final PerUserPoolDataSource pupds = new PerUserPoolDataSource();
		final ConnectionPoolDataSource cpds = pupds.getConnectionPoolDataSource();
		pupds.setConnectionPoolDataSource(cpds);

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					pupds.setDataSourceName("0]'");
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
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					pupds.close();
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
