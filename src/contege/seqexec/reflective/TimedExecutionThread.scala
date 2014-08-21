package contege.seqexec.reflective

import contege._

class TimedExecutionThread(val execute: () => ExecutionResult) extends Thread {

	var result: ExecutionResult = _
	
	var runFinished = true
	
	override def run = {
		runFinished = false
		
		// redirect stderr to stdout
		val oldStdErr = System.err
		System.setErr(System.out)		
		
		result = execute()
		
		// reset stderr
		System.setErr(oldStdErr)
		
		runFinished = true
	}	
	
}