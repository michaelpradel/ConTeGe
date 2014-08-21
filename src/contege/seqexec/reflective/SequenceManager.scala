package contege.seqexec.reflective

import scala.collection.mutable.Set
import contege.seqexec._
import contege.seqgen.AbstractCallSequence
import contege.Config
import contege.Finalizer

/**
 * Remembers which sequences have been executed to avoid
 * re-executing them.
 */
class SequenceManager(val seqExecutor: SequenceExecutor, config: Config, finalizer: Finalizer) {

	private val okSequences = Set[AbstractCallSequence[_]]()
	private val failingSequences = Set[AbstractCallSequence[_]]()
								   
    private var failingSeqs = 0
    private var succeedingSeqs = 0
								   
	def checkAndRemember(sequence: AbstractCallSequence[_]): Boolean = {
		// do we know the sequence already?
		if (okSequences.exists(oldSeq => sequence.equivalentTo(oldSeq))) {
		    succeedingSeqs += 1
			return true
		}
		if (failingSequences.exists(oldSeq => sequence.equivalentTo(oldSeq))) {
		    failingSeqs += 1
			return false
		}
		
		val errorOpt = seqExecutor.execute(sequence)
		    
		errorOpt match {
			case Some(msg) => { // error
				failingSequences.add(sequence)
				failingSeqs += 1
				return false
			}
			case None => {  // OK
				okSequences.add(sequence)
				succeedingSeqs += 1
				return true
			}
		}		
	}
	
	def printStats = {
	    println("Stats from SequenceManager: "+succeedingSeqs+" / "+failingSeqs+" (succeeding/failing)")
	}
	
}