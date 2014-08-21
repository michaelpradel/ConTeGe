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
 * Add a call of a CUT method and (if necessary)
 * calls to get arguments for the CUT call.
 */
class CallCutMethodTask(suffix: Suffix,
						cutMethods: Seq[MethodAtom],
						global: GlobalState) extends Task[Suffix](global) {

	override def run = {
		global.stats.callCutTasksStarted.incr
		val ret = super.run
		if (!ret.isDefined) global.stats.callCutTasksFailed.incr 
		ret		
	}
	
	def computeSequenceCandidate: Option[Suffix] = {
		var candidate = suffix.copy
		val cutMethod = cutMethods(global.random.nextInt(cutMethods.size))
		val receiver = candidate.prefix.getCutVariable
		assert(suffix.varsOfType(global.config.cut).contains(receiver))
	
		// create a subtask for each parameter; if one fails, this task also fails
		val args = new ArrayList[Variable]()
		
		cutMethod.paramTypes.foreach(typ => {
			val paramTask = new GetParamTask[Suffix](candidate, typ, global)			
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
		
		val retVal = cutMethod.returnType match {
			case Some(t) => Some(new ObjectVariable)
			case None => None
		} 
		
		val extendedCandidate = candidate.copy
		extendedCandidate.appendCall(cutMethod, Some(receiver), args, retVal)
		Some(extendedCandidate)
	}
	
}