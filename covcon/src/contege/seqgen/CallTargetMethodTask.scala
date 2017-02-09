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
import contege.PathTesterConfig

/**
 * Calls a specific method (the target method).
 */
class CallTargetMethodTask(prefix: Prefix,
                       	   global: GlobalState,
                       	   targetMethod: MethodAtom) extends Task[Prefix](global) {
	
    override def computeSequenceCandidate(): Option[Prefix] = { 
    	var candidate = prefix.copy

    	val receiver = if (targetMethod.isStatic) None
    				   else Some(prefix.getCutVariable)
    	
    	// create parameters to call the method (if needed)
    	// use user-provided parameter values
		val args = new ArrayList[Variable]()
	    val parameterValues = global.config.asInstanceOf[PathTesterConfig].targetMethodParameters
		
	    if (parameterValues.size == 0) {
	        for (argIdx <- 0 to targetMethod.paramTypes.length - 1) {
	        	appendArg(candidate, args, argIdx) match {
	        	    case Some(extendedSeq) => candidate = extendedSeq
	        	    case None => return None
	        	}
	        }
	    } else {
	        val parameters = global.random.chooseOne(parameterValues.toList)
	        assert(parameters.length == targetMethod.paramTypes.length)
	        for (argIdx <- 0 to parameters.length - 1) {
	        	parameters(argIdx) match {
	                case Some(param) => { // use user-provided parameter value
	                	args.add(param.getConstant)        
	                }
	                case None => { // unspecified -- try to find parameter value automatically
			            appendArg(candidate, args, argIdx) match {
			        	    case Some(extendedSeq) => candidate = extendedSeq
			        	    case None => return None
			        	}        
	                }
	            }
	        }
	    }
		
		val retVal = targetMethod.returnType match {
			case Some(t) => Some(new ObjectVariable)
			case None => None
		} 
    	
    	// append a call to the method
		val extendedCandidate = candidate.copy
		extendedCandidate.appendCall(targetMethod, receiver, args, retVal, None)
		
		Some(extendedCandidate)
    }
    
    // returns extended sequence (w/ calls to get one more argument) or None if no argument found
    def appendArg(candidate: Prefix, args: ArrayList[Variable], argIdx: Int): Option[Prefix] = {
        val typ = targetMethod.paramTypes(argIdx)
		val paramTask = new GetParamTask(candidate, typ, true, global)			
		paramTask.run match {
			case Some(extendedSequence) => {
				assert(paramTask.param.isDefined)
				args.add(paramTask.param.get)
				return Some(extendedSequence)
			}
			case None => {
				global.stats.callStateChangerFailedReasons.add("couldn't find param of type "+typ)
				return None
			}
		}
    }

}