package contege.seqexec.reflective

import scala.collection.mutable.Set
import contege.seqexec._
import contege.seqgen.AbstractCallSequence
import contege.Config
import contege.SubclassTesterConfig
import contege.SuperclassOracle
import contege.Finalizer

/**
 * Remembers which sequences have been executed to avoid
 * re-executing them.
 */
class SequenceManager(val seqExecutor: SequenceExecutor, config: Config, finalizer: Finalizer) {

	private val okSequences = Set[AbstractCallSequence[_]]()
	private val failingSequences = Set[AbstractCallSequence[_]]()
	private val superClassOracle = if (config.isInstanceOf[SubclassTesterConfig]) new SuperclassOracle(config.asInstanceOf[SubclassTesterConfig], seqExecutor, finalizer)
								   else null
	private val compareOutputVectors = if (config.isInstanceOf[SubclassTesterConfig]) config.asInstanceOf[SubclassTesterConfig].compareOutputVectors
	                                   else false
								   
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
		
		val (errorOpt, outputVectorOpt) = if (compareOutputVectors) {
		  val pair = seqExecutor.executeWithOutputVector(sequence)
		  (pair._1, Some(pair._2))
		} else (seqExecutor.execute(sequence), None)
		
		errorOpt match {
			case Some(msg) => { // error
				failingSequences.add(sequence)
				
				// test w/ superclass (if we are doing subclass testing)
				if (superClassOracle != null) {
				    superClassOracle.checkFailingSequence(sequence, msg)
				}
			    
				failingSeqs += 1
				return false
			}
			case None => {  // OK
				okSequences.add(sequence)
				
				// compare output vector to superclass (if enabled)
				if (compareOutputVectors && sequence.length != 0 /* call sequ. may be empty if first "call" gets a random primitive */) {
					superClassOracle.checkSucceedingSequence(sequence, outputVectorOpt.get)
				}
				
				succeedingSeqs += 1
				return true
			}
		}		
	}
	
	def printStats = {
	    println("Stats from SequenceManager: "+succeedingSeqs+" / "+failingSeqs+" (succeeding/failing)")
	}
	
}