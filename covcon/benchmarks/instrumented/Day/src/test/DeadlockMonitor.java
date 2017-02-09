package test;


import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class DeadlockMonitor extends Thread {
	
	private ThreadMXBean threadMgr = ManagementFactory.getThreadMXBean();
	
	/**
	 * Checks periodically for deadlocks and reports them.
	 */
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long[] threads = threadMgr.findDeadlockedThreads();
			if (threads != null) {  // found a deadlock
				System.out.println("\n ============================= Deadlock =============================");
				ThreadInfo[] threadInfos = threadMgr.getThreadInfo(threads, true, true);
				for (int i = 0; i < threadInfos.length; i++) {
					ThreadInfo threadInfo = threadInfos[i];
					System.out.println(threadInfo);
				}
				System.out.println("=====================================================================");
				System.exit(0);
			}
		}
	}

}