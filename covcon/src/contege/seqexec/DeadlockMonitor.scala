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
			Thread.sleep(1000);
			val threads = threadMgr.findDeadlockedThreads
			if (threads != null) {  // found a deadlock
			    config.checkerListeners.foreach(_.appendResultMsg("\n =========Found a thread safety violation (deadlock)! ========" ))
			    config.checkerListeners.foreach(_.appendResultMsg("Exception Found : Deadlock"))
				val threadInfos = threadMgr.getThreadInfo(threads, true, true)
				threadInfos.foreach(info => config.checkerListeners.foreach(_.appendResultMsg(info.toString)))
				config.checkerListeners.foreach(_.appendResultMsg("=====================================================================" ))
				
				global.finalizer.finalizeAndExit(true)
			}
		}
	}

}