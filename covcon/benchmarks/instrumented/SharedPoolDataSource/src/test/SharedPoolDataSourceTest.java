package test;

import java.util.ConcurrentModificationException;

import javax.sql.ConnectionPoolDataSource;

import org.apache.commons.dbcp.datasources.SharedPoolDataSource;

public class SharedPoolDataSourceTest {

	public static void main(String[] args) {
		new SharedPoolDataSourceTest().run();
	}
	
	private void run() {
		final SharedPoolDataSource spds = new SharedPoolDataSource();
		final ConnectionPoolDataSource cpds = spds.getConnectionPoolDataSource();
		spds.setConnectionPoolDataSource(cpds);

		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					spds.close();
				} catch (Throwable t) {
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					spds.setDataSourceName("aaa");
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
