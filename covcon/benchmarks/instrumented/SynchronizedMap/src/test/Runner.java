package test;

import clinitrewriter.Clinit;

public class Runner {

	public static void main(String[] args) {
		DeadlockMonitor monitor = new DeadlockMonitor();
		monitor.setDaemon(true);
		monitor.start();
		for (int i = 0; i < 1000000; i++) {
			SynchronizedMapTest.main(args);
			Clinit.reset(); // reset static state of classes under test
		}
	}
	
}
