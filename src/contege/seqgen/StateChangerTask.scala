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
import contege.MethodAtom

/**
 * Calls a method on a given variable
 * (hoping that the call influences the state of the receiver object).
 */
class StateChangerTask(prefix: Prefix,
                       global: GlobalState) extends Task[Prefix](global) {
	
    var targetMethod: Option[MethodAtom] = None
    
    override def computeSequenceCandidate(): Option[Prefix] = { 
    	var candidate = prefix.copy
    	
    	// choose a method to call (or use the target method)
    	val selectedMethod = if (targetMethod.isDefined) targetMethod.get else global.random.chooseOne(global.typeProvider.cutMethods)

    	val receiver = Some(prefix.getCutVariable)
    	
    	// create parameters to call the method (if needed)
    	// --> one subtask per parameter; if one fails, this task also fails
		val args = new ArrayList[Variable]()
		
		selectedMethod.paramTypes.foreach(typ => {
			val paramTask = new GetParamTask(candidate, typ, true, global)			
			paramTask.run match {
				case Some(extendedSequence) => {
					candidate = extendedSequence
					assert(paramTask.param.isDefined)
					args.add(paramTask.param.get)
				}
				case None => {
					global.stats.callStateChangerFailedReasons.add("couldn't find param of type "+typ)
					return None
				}
			}
		})
		
		val retVal = selectedMethod.returnType match {
			case Some(t) => Some(new ObjectVariable)
			case None => None
		} 
    	
    	// append a call to the method
		val extendedCandidate = candidate.copy
		extendedCandidate.appendCall(selectedMethod, receiver, args, retVal, None)
		
		Some(extendedCandidate)
    }

}