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
 * Adds a CUT call to a suffix. Before using this task, the
 * PrepareCUTCallTask must prepare it by adding calls to the prefix 
 * that provide all required arguments.
 */
class AppendPreparedCUTCallTask(suffix: Suffix,
		                        method: MethodAtom,
		                        args: ArrayList[Variable],
		                        global: GlobalState) extends Task[Suffix](global) {

	override def computeSequenceCandidate: Option[Suffix] = {
		val candidate = suffix.copy
		val receiver = suffix.prefix.getCutVariable
		
		val retVal = method.returnType match {
			case Some(t) => Some(new ObjectVariable)
			case None => None
		}
		
		candidate.appendCall(method, Some(receiver), args, retVal, None)
		Some(candidate)
	}
	
}