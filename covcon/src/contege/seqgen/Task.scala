package contege.seqgen

import contege.Timer
import contege.Config
import contege.Stats
import contege.GlobalState
import contege.PathTesterConfig

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
          val successful = global.seqMgr.checkAndRemember(cand)
          if (successful) {
            return Some(cand)
          }
        }
        case None => {
          // continue search
        }
      }
    }
    println("Reached maxTries (" + maxTries + ")")
    return None
  }

  /**
   * Returns a sequence candidate (which may fail when executed)
   * or none of no candidate was found.
   */
  def computeSequenceCandidate: Option[CallSequence]

}