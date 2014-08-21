package contege.seqexec.reflective

import contege._

object TimeoutRunner {

	val timeout = 10000 // milliseconds
	
	def runWithTimeout(execute: () => ExecutionResult, description: String): ExecutionResult = {
		val execThread = new TimedExecutionThread(execute)
		execThread.start
		execThread.join(timeout)
		if (!execThread.runFinished) {
			execThread.stop
			println("Timeout when running "+description)
			Console.out.flush
			return Exception(TimeoutException)
		}
		assert(execThread.result != null)
		return execThread.result
	}
	
}

object TimeoutException extends RuntimeException