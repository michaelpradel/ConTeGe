package contege.seqexec.reflective

import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch;
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import scala.collection.JavaConversions.asScalaBuffer
import contege.seqexec.TSOracle
import contege.seqgen.Prefix
import contege.seqgen.Suffix
import contege.Config
import contege.Finalizer
import contege.SequentialInterleavings
import contege.Stats
import contege.GlobalState
import scala.actors.threadpool.TimeUnit

class TSOracleNormalExec(finalizer: Finalizer, stats: Stats, executor: SequenceExecutor,
	config: Config, global: GlobalState) extends TSOracle(finalizer, stats, config) {

	override def analyzeTest(prefix: Prefix, suffix1: Suffix, suffix2: Suffix) = {

	}

	//val baseRunningTime = 5000000000L
	val baseRunningTime = 10000000000L
	//val baseRunningTime = 2000000000L

	def getRepsCount(version: Int, prefix: Prefix, suffix1: Suffix, suffix2: Suffix, suffixRuns: Int, nrThreads: Int, multiplier: Double): Int = {
		var runningTime = 0L
		var warmUpReps = 0
		var exceptions = 0
		var timeoutsCount = 0
		val maxRunningTime = (baseRunningTime * multiplier).toLong

		while (runningTime < maxRunningTime) {
			warmUpReps += 1
			val start = System.nanoTime()
			val (runTime, valid, timeouts) = executor.executeConcurrently(stats, prefix, suffix1, suffix2, 0, suffixRuns, nrThreads)
			val stop = System.nanoTime()
			runningTime += (stop - start)
			if (!valid) {
				exceptions += 1
			}
			if (timeouts) {
				timeoutsCount += 1
			}
		}
		if (timeoutsCount > 0) {
			println(" --- There were timeouts while determining reps count for version " + version)
			return -2
			//} else if (exceptions > (warmUpReps / 2)) {
		} else if (exceptions > 0) {
			println(" --- There were exceptions while determining reps count for version " + version)
			return -1
		} else {
			println(" --- Targeted running time: " + TimeUnit.SECONDS.convert(maxRunningTime, TimeUnit.NANOSECONDS) + "s -> " + warmUpReps + " repetitions for version " + version + " (" + nrThreads + " threads)")
			return warmUpReps
		}
	}

	def analyzeTestForPerformance(version: Int, prefix: Prefix, suffix1: Suffix, suffix2: Suffix, runID: String, suffixRuns: Int, nrThreads: Int, referenceRepetitions: Int, multiplier: Double): Long = {

		val warmUpSecs = TimeUnit.SECONDS.convert(baseRunningTime.toLong, TimeUnit.NANOSECONDS)
		//var runningTimeSecs = TimeUnit.SECONDS.convert((baseRunningTime * multiplier).toLong, TimeUnit.NANOSECONDS) * 2
		var runningTimeSecs = TimeUnit.SECONDS.convert((baseRunningTime * multiplier).toLong, TimeUnit.NANOSECONDS)

		//val warmUpRepetitions = List((referenceRepetitions / multiplier).toInt, 50).max
		val warmUpRepetitions = List((referenceRepetitions / multiplier).toInt, 10).max
		//var repetitions = 2 * referenceRepetitions
		//var repetitions = (warmUpRepetitions + referenceRepetitions)
		var repetitions = List(warmUpRepetitions, referenceRepetitions).max

		println("==== Running Version " + version + ": GC (~10s), warm up (" + warmUpRepetitions + " reps, ~" + warmUpSecs + "s), steady * 3 (" + repetitions + ", ~" + runningTimeSecs + "s), total: ~ " + (10 + warmUpSecs + runningTimeSecs * 3) + "s ====")
		
		forceGC()

		var valid = true
		var rep = 0
		while (rep < warmUpRepetitions && valid) {
			stats.timer.start("conc_exec")
			rep += 1
			val (runTime, lvalid, timeouts) = executor.executeConcurrently(stats, prefix, suffix1, suffix2, rep, suffixRuns, nrThreads)
			if (!lvalid || timeouts) {
				valid = false
			}
			stats.timer.stop("conc_exec")
		}
		
		val maxTries = 5
		var tries = 0
		
		var targetThreshold = config.targetThresholdQuiet
		var acceptableThreshold = config.acceptableThresholdQuiet
		if (config.noisyEnvironment) {
			targetThreshold = config.targetThresholdNoisy
			acceptableThreshold = config.acceptableThresholdNoisy
		}
		
		var t1 = runTest(repetitions, prefix, suffix1, suffix2, suffixRuns, nrThreads)
		var t2 = runTest(repetitions, prefix, suffix1, suffix2, suffixRuns, nrThreads)
		var t3 = runTest(repetitions, prefix, suffix1, suffix2, suffixRuns, nrThreads)
		
		var runningTimes = List[Long]()
		runningTimes ::= t1 
		runningTimes ::= t2 
		runningTimes ::= t3 
		
		var mean = avg(runningTimes)
		var threshold = mean * targetThreshold
		var stdd = stdDev(runningTimes, mean)
		
		println("\n  mean     : " + mean + " (" + runningTimes.toString + ")")
		println("  threshold: " + threshold + " ( " + targetThreshold + ")")
		println("  stdd     : " + stdd)
		
		while (allValid(runningTimes) && tries < maxTries && stdd > threshold) {
			tries += 1
			println("\n    --- Standard deviation too large, running again, try " + tries + " of " + maxTries + " (another ~" + runningTimeSecs + "s)\n")
			var t = runTest(repetitions, prefix, suffix1, suffix2, suffixRuns, nrThreads)
			runningTimes ::= t
			mean = avg(runningTimes)
			threshold = mean * targetThreshold
			stdd = stdDev(runningTimes, mean)
			println("  mean     : " + mean + " (" + runningTimes.toString + ")")
			println("  threshold: " + threshold + " (" + targetThreshold + ")")
			println("  stdd     : " + stdd)
		}
		if (allValid(runningTimes)) {
			
			if (tries >= maxTries && stdd > (mean * acceptableThreshold)) {
				println()
				println("==== Too many tries and stdd larger than acceptable threshold " + (mean * acceptableThreshold) + ", discarding run ====")
				println()
				0L
			} else {
				stats.performanceTimer.recordTestRunSeries(version, prefix, suffix1, suffix2, runID, runningTimes, nrThreads)
				stats.performanceTimer.recordAvgTestRun(version, prefix, suffix1, suffix2, runID, mean.toLong, nrThreads)
				println()
				println("==== Runs valid, threshold " + (mean * acceptableThreshold) + " (" + (acceptableThreshold * 100) + "%) ok , mean is " + mean + " ====")
				println()
				mean.toLong
			}
		} else {
			println()
			println("==== Not all runs werde valid, discarding test run ====")
			println()
			0L
		}
	}
	
	private def runTest(repetitions: Int, prefix: Prefix, suffix1: Suffix, suffix2: Suffix, suffixRuns: Int, nrThreads: Int): Long = {
		var totalRunningTime = 0L
		var valid = true
		var rep = 0
		while (rep < repetitions && valid) {
			stats.timer.start("conc_exec")
			rep += 1
			val (runTime, lvalid, timeouts) = executor.executeConcurrently(stats, prefix, suffix1, suffix2, rep, suffixRuns, nrThreads)
			if (lvalid && !timeouts) {
				if ((totalRunningTime + runTime) < totalRunningTime) {
					println(" >>> WARNING: Overflow in TSOralceNormalExec")
					valid = false
				} else {
					totalRunningTime += runTime
				}
			} else {
				valid = false
			}
			stats.timer.stop("conc_exec")
		}
		if (valid) {
			TimeUnit.MILLISECONDS.convert(totalRunningTime, TimeUnit.NANOSECONDS)
		} else {
			0L
		}
	}
	
	private def avg(runningTimes: List[Long]): Double = {
      	runningTimes.foldLeft(0L)(_+_).toDouble / runningTimes.length.toDouble
	}
	
	private def stdDev(runningTimes: List[Long], avg: Double): Double = runningTimes match {
		case Nil  => 0.0
		case list => math.sqrt((0.0 /: list) {
			(sum, item) => sum + math.pow(item - avg, 2.0)
		} / (runningTimes.size - 1))
	}
	
	private def allValid(runningTimes: List[Long]): Boolean = {
		runningTimes.forall(item => item > 0)
	}

	private def forceGC() {
		// this method is taken from the caliper micro benchmarking suite: com.google.caliper.util.Util.forceGc()
		// https://code.google.com/p/caliper/source/browse/caliper/src/main/java/com/google/caliper/util/Util.java
		System.gc()
		System.runFinalization()
		val latch = new CountDownLatch(1);
		new Object() {
			override def finalize() {
				latch.countDown()
			}
		};
		System.gc()
		System.runFinalization()
		try {
			latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
		} catch {
			case e: InterruptedException => Thread.currentThread().interrupt()
		}
		Thread.sleep(2000)
	}

	private def sameExceptionKind(outcome1: Throwable, outcome2: Throwable): Boolean = {
		if (outcome1.isInstanceOf[InvocationTargetException] && outcome2.isInstanceOf[InvocationTargetException]) {
			val realMsg1 = outcome1.asInstanceOf[InvocationTargetException].getCause
			val realMsg2 = outcome2.asInstanceOf[InvocationTargetException].getCause
			return realMsg1.getClass.getName == realMsg2.getClass.getName
		} else {
			outcome1.getClass.getName == outcome2.getClass.getName
		}

	}
}