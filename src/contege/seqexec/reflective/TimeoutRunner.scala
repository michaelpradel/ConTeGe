package contege.seqexec.reflective

import contege._

object TimeoutRunner {

	val timeout = 20000 // milliseconds

	def runWithTimeout(execute: () => ExecutionResult, description: String): ExecutionResult = {
		val execThread = new TimedExecutionThread(execute)
		execThread.start
		execThread.join(timeout)
		if (!execThread.runFinished) {
			execThread.stop
			println("Timeout when running " + description)
			Console.out.flush
			return Exception(TimeoutException)
		}
		if (execThread.result == null) {
			println("Warning thread result was 'null'")
			return Exception(ThreadResultNull)
		}
		return execThread.result
	}

}

object TimeoutException extends RuntimeException

object ThreadResultNull extends RuntimeException