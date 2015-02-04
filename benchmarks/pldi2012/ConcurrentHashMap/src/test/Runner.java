package test;

public class Runner {

	public static void main(String[] args) {
		DeadlockMonitor monitor = new DeadlockMonitor();
		monitor.setDaemon(true);
		monitor.start();
		for (int i = 0; i < 1000000; i++) {
			ConcurrentHashMapTest.main(args);
		}
	}
	
}
