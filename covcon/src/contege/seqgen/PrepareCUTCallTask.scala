package contege.seqgen

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import contege.ClassReader
import contege.Random
import contege.Atom
import contege.ConstructorAtom
import contege.MethodAtom
import contege.Stats
import contege.Config
import contege.GlobalState

/**
 * Adds calls that provide parameters required to call a 
 * CUT method. This task is used to add parameter-providing calls 
 * to the prefix before calling the CUT method in the suffix.
 */
class PrepareCUTCallTask(prefix: Prefix,
		                 cutMethod: MethodAtom,
		                 global: GlobalState) extends Task[Prefix](global) {

	// filled by computeSequenceCandidate;
	// if we don't find a passing sequence, set to None
	var args: Option[ArrayList[Variable]] = None
	
	private var candidateWithoutCall: Option[Prefix] = None
	
	override def run = {
		val ret = super.run
		if (ret.isDefined) {
			assert(candidateWithoutCall.isDefined)
			// found parameters that make the CUT call possible -- return the sequence w/o the call (will be done in suffix)
			candidateWithoutCall	
		} else {
			// did not find a sequence
			args = None
			None
		}
	}
	
	override def computeSequenceCandidate: Option[Prefix] = {
		var candidate = prefix.copy
		
		val args = new ArrayList[Variable]()

		cutMethod.paramTypes.foreach(typ => {
			val paramTask = new GetParamTask[Prefix](candidate, typ, true, global)			
			paramTask.run match {
				case Some(extendedSequence) => {
					candidate = extendedSequence
					assert(paramTask.param.isDefined)
					args.add(paramTask.param.get)
				}
				case None => {
					global.stats.callCutFailedReasons.add("couldn't find param of type "+typ)
					return None
				}
			}
		})
		
		this.args = Some(args)
		candidateWithoutCall = Some(candidate)
		
		val retVal = cutMethod.returnType match {
			case Some(t) => Some(new ObjectVariable)
			case None => None
		} 
		
		val extendedCandidate = candidate.copy
		val receiver = prefix.getCutVariable
		extendedCandidate.appendCall(cutMethod, Some(receiver), args, retVal, None)
		Some(extendedCandidate)
	}
	
}