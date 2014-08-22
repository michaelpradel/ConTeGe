package contege.seqexec.reflective

import scala.collection.JavaConversions._

import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.{ List => JList }
import java.util.ArrayList
import java.util.Collections
import contege._
import contege.seqgen._
import contege.seqexec._
import clinitrewriter.Clinit
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import scala.actors.threadpool.TimeUnit
import java.util.concurrent.Future
import scala.actors.threadpool.CancellationException

/**
 * Executes tests reflectively.
 */
class SequenceExecutor(stats: Stats, config: Config) {

	def execute(seq: AbstractCallSequence[_]): Option[Throwable] = {
		if (seq.isInstanceOf[Prefix]) {
			stats.executedSequences.incr
			if (config.callClinit) Clinit.reset
			val result = seq.execute
			result
		} else if (seq.isInstanceOf[Suffix]) {
			val suffix = seq.asInstanceOf[Suffix]
			stats.executedSequences.incr
			if (config.callClinit) Clinit.reset

			val var2Object = Map[Variable, Object]()
			val prefixResult = suffix.prefix.execute(var2Object)
			if (!prefixResult.isEmpty) {
				println("prefix fails when used with suffix -- this shouldn't happen...")
				println("************************")
				println("prefix " + suffix.prefix)
				println(">>>>>>>>>>>>>>>>>>>>>>>>")
				println("suffix: " + suffix)
				println("************************")
				return prefixResult
			}
			assert(!var2Object.isEmpty, "At least the CUT object should be here: " + var2Object)
			suffix.execute(var2Object)
		} else throw new IllegalArgumentException("unexpected subtype " + seq.getClass.getName)
	}

	def executeWithOutputVector(seq: AbstractCallSequence[_]): (Option[Throwable], OutputVector) = {
		if (seq.isInstanceOf[Prefix]) {
			stats.executedSequences.incr
			if (config.callClinit) Clinit.reset
			seq.executeWithOutputVector
		} else if (seq.isInstanceOf[Suffix]) {
			val suffix = seq.asInstanceOf[Suffix]
			stats.executedSequences.incr
			if (config.callClinit) Clinit.reset

			val var2Object = Map[Variable, Object]()
			val outputVector = new OutputVector
			val (prefixResult, prefixOutputVector) = suffix.prefix.executeWithOutputVector(var2Object, outputVector)
			if (!prefixResult.isEmpty) {
				println("prefix fails when used with suffix -- this shouldn't happen...")
				return (prefixResult, prefixOutputVector)
			}
			assert(!var2Object.isEmpty, "At least the CUT object should be here: " + var2Object)
			suffix.executeWithOutputVector(var2Object, outputVector)
		} else throw new IllegalArgumentException("unexpected subtype " + seq.getClass.getName)
	}

	/**
	 * Returns None if the execution passes and a message containing the reason for failure otherwise.
	 */
	def executeConcurrently(stats: Stats, prefix: Prefix, suffix1: Suffix,
		suffix2: Suffix, rep: Int, suffixRuns: Int, nrThreads: Int): (Long, Boolean, Boolean) = {
		assert(prefix == suffix1.prefix && suffix1.prefix == suffix2.prefix)
		
		if (config.callClinit) Clinit.reset

		//val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
		val pool = Executors.newFixedThreadPool(nrThreads)

		stats.concurrentRuns.incr

		var totalRunTime = 0L
		var valid = true
		var timeouts = false

		val var2Object = Map[Variable, Object]()
		val prefixResult = prefix.execute(var2Object)
		if (!prefixResult.isEmpty) {
			// this shouldn't happen (normally) - but can happen, e.g. if there are too many open files now, or if the sequential execution is non-deterministic
			// --> behave as if the concurrent run was OK (we can't show it to cause an exception)
			println("prefix failed or timed out, this should not happen")
			timeouts = true
			valid = false
		}

		var start = 0L
		var stop = 0L

		if (valid && !var2Object.isEmpty) {

			val threads = new ArrayList[Callable[Boolean]]()

			for (i <- 0 to (nrThreads - 1)) {
				val var2ObjectT = var2Object.clone
				if (i % 2 == 0) {
					threads.add(new Callable[Boolean]() {
						def call() = {
							var lvalid = true
							var run = 0
							while (run < suffixRuns && lvalid) {
								val tvalid = suffix1.executeForPerformance(var2ObjectT)
								if (!tvalid) {
									lvalid = false
								}
								run += 1
							}
							lvalid
						}
					})
				} else {
					threads.add(new Callable[Boolean]() {
						def call() = {
							var lvalid = true
							var run = 0
							while (run < suffixRuns && lvalid) {
								val tvalid = suffix2.executeForPerformance(var2ObjectT)
								if (!tvalid) {
									lvalid = false
								}
								run += 1
							}
							lvalid
						}
					})
				}

			}

			var results: java.util.List[Future[Boolean]] = new ArrayList[Future[Boolean]]()

			try {
				//start = System.currentTimeMillis();
				start = System.nanoTime()
				stop = start
				results = pool.invokeAll(threads, 5000, java.util.concurrent.TimeUnit.MILLISECONDS)
				//stop = System.currentTimeMillis();
				stop = System.nanoTime()
			} catch {
				case ie: InterruptedException => {
					println("************************")
					println("Interrupted exception thrown")
					// results size should be 0, not counting
					// we should report that and not count the other versions corresponding run
					println("results size: " + results.size())
					println("************************")
					timeouts = true
				}
				case e => {
					println("************************")
					println("exception: " + e.getMessage())
					println("results size: " + results.size())
					println(e.printStackTrace())
					println("************************")
					valid = false
				}
			}

			// collect all the running times
			results.foreach(result => {
				var threadValid = true
				try {
					threadValid = result.get(5, java.util.concurrent.TimeUnit.SECONDS)
				} catch {
					case ce: java.util.concurrent.CancellationException => {
						timeouts = true
					}
					case e => {
						valid = false
					}
				}
				if (!threadValid) {
					valid = false
				}
			})

			pool.shutdown()
			if (!pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
				println(" >>> WARNING: Had to abort pool termination due to timeout!")
			}
		} else {
			println("prefix is invalid!")
		}

		//totalRunTime = TimeUnit.MICROSECONDS.convert((stop - start), TimeUnit.NANOSECONDS)
		totalRunTime = (stop - start)
		//totalRunTime = checkDuration(stop - start)

		return (totalRunTime, (totalRunTime > 0 && valid), timeouts)
	}
	
	def checkDuration(value: Long): Long = {
		var result = 0L
	    if (value < 0) {
	    	println(" >>> WARNING: negative running time!!!")
	    	result = 0L
	    } else if (value >= TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS)){
	    	println(" >>> WARNING: running time too long (more than 60 seconds)!!!")
	    	result = 0L
	    } else {
	    	result = value 
	    }
	    return result
	}

}
