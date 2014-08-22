package contege.seqexec

import java.lang.management._
import contege.ClassTester
import contege.Config
import scala.collection.mutable.Map
import contege.Finalizer
import contege.GlobalState

class DeadlockMonitor(config: Config, global: GlobalState) extends Thread {
	
	private val threadMgr = ManagementFactory.getThreadMXBean;
	
	/**
	 * Checks periodically for deadlocks and reports them.
	 */
	override def run() {
		while (true) {
			Thread.sleep(60000);
			val threads = threadMgr.findDeadlockedThreads
			if (threads != null) {  // found a deadlock
			    global.debug("\n =========Found a thread safety violation (deadlock)! ========", 1)
				val threadInfos = threadMgr.getThreadInfo(threads, true, true)
				threadInfos.foreach(info => {
					global.debug(info.toString, 1)
				})
				global.debug("=====================================================================", 1)
			}
		}
	}

}