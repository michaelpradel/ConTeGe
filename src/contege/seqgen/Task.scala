package contege.seqgen

import contege.Timer
import contege.Config
import contege.Stats
import contege.GlobalState
import scala.util.MurmurHash

/**
 * Some part of work to do for generating tests.
 * Tries to accomplish the task a configurable nb of
 * times and returns an extended call sequence if a sequence
 * is found that executes without crashes.
 */
abstract class Task[CallSequence <: AbstractCallSequence[_]](global: GlobalState) {

	private val maxTries = 10

	def run: Option[CallSequence] = {
		var triesLeft = maxTries
		while (triesLeft > 0) {
			triesLeft -= 1
			computeSequenceCandidate match {
				case Some(cand) => {
					global.debug("cand: " + MurmurHash.stringHash(cand.toString).toString, 3)
					global.debug(" -- Running candidate (" + this.getClass.getSimpleName + "):\n" + cand, 3)
					global.stats.timer.start("candExec")
					val successful = global.seqMgr.checkAndRemember(cand)
					global.stats.timer.stop("candExec")
					if (successful) {
						global.debug(" -- succeeds", 3)
						return Some(cand)
					} else {
						global.debug(" -- failed\n", 2)
					}
				}
				case None => {
					// continue search
				}
			}
		}
		global.debug(" -- Could not compute a sequence candidate", 1)
		return None
	}

	/**
	 * Returns a sequence candidate (which may fail when executed)
	 * or none of no candidate was found.
	 */
	def computeSequenceCandidate: Option[CallSequence]

}