package contege.seqgen

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import contege._

/**
 * Adds calls to instantiate the CUT (e.g. a call to a constructor
 * and calls to provide arguments for the constructor call).
 */
class InstantiateCutTask(global: GlobalState) extends Task[Prefix](global) {
		
	def computeSequenceCandidate: Option[Prefix] = {
		val cut = global.config.cut
		val constructingAtoms = new ArrayList[Atom]
		val constructors = global.typeProvider.constructors(cut)
		if (global.config.isInstanceOf[SubclassTesterConfig]) {
			// use only constructors that can be mapped to subclass constructors
			val subclassTesterConfig = global.config.asInstanceOf[SubclassTesterConfig]
			val mappableConstructors = constructors.filter(c => subclassTesterConfig.constructorMap.mappable(c.paramTypes.mkString("(",",",")")))
			constructingAtoms.addAll(mappableConstructors) 
		} else {
		    // use all constructors and methods that can instantiate the CUT
			constructingAtoms.addAll(constructors)		    
			val ownStaticCreators = global.typeProvider.cutMethods.filter(m => m.isStatic && m.returnType.isDefined && m.returnType.get == cut)
			constructingAtoms.addAll(ownStaticCreators)
			if (global.config.isInstanceOf[PathTesterConfig]) {
			    // use all methods that return an instance of the CUT
				val otherCreators = global.typeProvider.allAtomsGivingType(cut)
				constructingAtoms.addAll(otherCreators)    
			} else {
			    // use static methods only (why? mostly to preserve ClassTester's behavior as described in PLDI'12) 
			    val otherStaticCreators = global.typeProvider.allAtomsGivingType(cut).filter(atom => atom.isMethod && atom.isStatic)
				constructingAtoms.addAll(otherStaticCreators)
			} 
		}
		
		if (constructingAtoms.isEmpty) {
			println("No constructor or method to create instance of CUT! "+cut)
			return None
		}
		
		val constructor = constructingAtoms(global.random.nextInt(constructingAtoms.size))

		var result = new Prefix(global)  // create a fresh sequence
		
		// if the "constructor" is an instance method, create a subtask to find an instance
		var receiver: Option[Variable] = None
		if (!constructor.isStatic && !constructor.isConstructor) {
		    val receiverType = constructor.declaringType
		    val receiverTask = new GetParamTask(result, receiverType, false, global)
		    receiverTask.run match {
		        case Some(extendedSequence) => {
		            result = extendedSequence
		            assert(receiverTask.param.isDefined)
		            receiver = receiverTask.param
		        }
		        case None => return None
		    }
		}
		
		// create a subtask for each parameter; if one fails, this task also fails
		val args = new ArrayList[Variable]()
		constructor.paramTypes.foreach(typ => {
			val paramTask = new GetParamTask(result, typ, true, global)
			paramTask.run match {
				case Some(extendedSequence) => {
					result = extendedSequence
					assert(paramTask.param.isDefined)
					args.add(paramTask.param.get)
				}
				case None => {
					return None
				}
			}
		})
		
		result.appendCall(constructor, receiver, args, Some(new ObjectVariable), None, true)
		
		Some(result)
	}

}