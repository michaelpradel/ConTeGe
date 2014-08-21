package contege.seqexec.reflective

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.{List => JList}
import java.util.ArrayList
import java.util.Collections
import contege._
import contege.seqgen._
import contege.seqexec._

import clinitrewriter.Clinit

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
				return prefixResult
			}
			assert(!var2Object.isEmpty, "At least the CUT object should be here: "+var2Object)
			suffix.execute(var2Object)
		} else throw new IllegalArgumentException("unexpected subtype "+seq.getClass.getName)
	}
	
	/**
	 * Returns None if the execution passes and a message containing the reason for failure otherwise.
	 */
	def executeConcurrently(prefix: Prefix,
							suffix1: Suffix,
							suffix2: Suffix): JList[Throwable] = {
		assert(prefix == suffix1.prefix && suffix1.prefix == suffix2.prefix)
		
		stats.concurrentRuns.incr
				
		if (config.callClinit) Clinit.reset
		
		val result = Collections.synchronizedList(new ArrayList[Throwable]())
		
		val var2Object = Map[Variable, Object]()
		val prefixResult = prefix.execute(var2Object)
		if (!prefixResult.isEmpty) {
			// this shouldn't happen (normally) - but can happen, e.g. if there are too many open files now, or if the sequential execution is non-deterministic
			// --> behave as if the concurrent run was OK (we can't show it to cause an exception)
			assert(result.isEmpty)
			return result
		}
		assert(!var2Object.isEmpty, "\nThere must be a CUT object at least!\nPrefix:\n"+prefix)
		val var2ObjectT1 = var2Object.clone
		val var2ObjectT2 = var2Object.clone
		
		var unexpectedException: Option[Throwable] = None
		val t1 = new Thread() {
			override def run {
				try {
					val msg = suffix1.execute(var2ObjectT1)
					if (msg.isDefined) result.add(msg.get)
				} catch {
					case e: Throwable => unexpectedException = Some(e)
				}
			}
		}
		val t2 = new Thread() {
			override def run {
				try {
					val msg = suffix2.execute(var2ObjectT2)
					if (msg.isDefined) result.add(msg.get)
				} catch {
					case e: Throwable => unexpectedException = Some(e)
				}
			}
		}
		
		t1.start
		t2.start
		t1.join
		t2.join

		// propagate unexpected exceptions (e.g. violations of assertions in ConTeGe) to the main thread
		if (unexpectedException.isDefined) throw unexpectedException.get
		
		if (!result.isEmpty) stats.failedConcRuns.incr
		else stats.succeededConcRuns.incr
		
		result
		
	}
		
}
