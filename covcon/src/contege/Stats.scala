package contege

import scala.collection.mutable.Map

class Stats {

    val timer = new Timer
  
	val executedSequences = new IncrementableCounter
	val failedSequenceExecutions = new IncrementableCounter
	
	val concurrentRuns = new IncrementableCounter
	
	val failedConcRuns = new IncrementableCounter
	
	val genTests = new IncrementableCounter
	
	val succeededConcRuns = new IncrementableCounter
	
	val sequentialInterleavings = new IncrementableCounter
	
	val tooLongSeqs = new IncrementableCounter
	
	val paramTasksStarted = new FreqCounter
	val paramTasksFailed = new FreqCounter
	
	val callCutTasksStarted = new IncrementableCounter
	val callCutTasksFailed = new IncrementableCounter
	val callCutFailedReasons = new FreqCounter
	val callStateChangerFailedReasons = new FreqCounter
	
	val nullParams = new FreqCounter
	
	val noParamFound = new FreqCounter
	
    val succJPFRuns = new IncrementableCounter
    val inconclusiveJPFRuns = new IncrementableCounter
	
	def print = {
		println("\n=== Statistics:  ===")
		println("Generated Tests Count         : "+genTests.get)
		println("Executed sequences Count      : "+executedSequences.get)
		println("   FailedSeqs Count           : "+failedSequenceExecutions.get)
		println("Concurrent runs Count         : "+concurrentRuns.get)
		println("   SucceededConcRuns Count    : "+succeededConcRuns.get)
		println("   FailedConcRuns Count       : "+failedConcRuns.get)
		println("Sequential interleavings Count: "+sequentialInterleavings.get)
		println("------------")
		println("Too long sequence: "+tooLongSeqs.get)
		println("Failed call CUT tasks: "+callCutTasksFailed.get+" / "+callCutTasksStarted.get)
		println("Reasons for failed call CUT tasks:")
		callCutFailedReasons.printTop
		println("Reasons for failed state changer call tasks:")
		callStateChangerFailedReasons.printTop
		println("Tasks started:")
		paramTasksStarted.printTop
		println("Tasks failed:")
		paramTasksFailed.printTop
		println("Null parameters:")
		nullParams.printTop
		println("No parameter found:")
		noParamFound.printTop
		println("\n-------------------------\nSuccessful / inconclusive JPF runs: "+succJPFRuns.get+" / "+inconclusiveJPFRuns.get)
		println("====================")
	}	
}

class IncrementableCounter {
	private var ctr = 0L
	
	def get = ctr
	
	def incr = {
		ctr += 1
	}
	
	def add(i: Int) = {
		ctr += i
	}
}

class FreqCounter {
	private val string2Freq = Map[String, Long]()
	
	def add(s: String) = {
		string2Freq.put(s, (1+string2Freq.getOrElse(s, 0L)).toLong)
	}
	
	def printTop = {
		string2Freq.toList.sortWith((e1, e2) => e1._2 > e2._2).take(5).foreach(pair => {
			println("   "+pair._2+" -- "+pair._1)
		})
	}
}