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
	    // use all constructors and methods that can instantiate the CUT
		constructingAtoms.addAll(constructors)		    
		val ownStaticCreators = global.typeProvider.cutMethods.filter(m => m.isStatic && m.returnType.isDefined && m.returnType.get == cut)
		constructingAtoms.addAll(ownStaticCreators)
		val otherStaticCreators = global.typeProvider.allAtomsGivingType(cut).filter(atom => atom.isMethod && atom.isStatic)
		constructingAtoms.addAll(otherStaticCreators)    
		
		if (constructingAtoms.isEmpty) {
			println("No constructor or method to create instance of CUT! "+cut)
			return None
		}
		
		val constructor = constructingAtoms(global.random.nextInt(constructingAtoms.size))

		var result = new Prefix(global)  // create a fresh sequence
		
		// create a subtask for each parameter; if one fails, this task also fails
		val args = new ArrayList[Variable]()
		constructor.paramTypes.foreach(typ => {
			val paramTask = new GetParamTask(result, typ, global)
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
		
		result.appendCall(constructor, None, args, Some(new ObjectVariable), true)
		
		Some(result)
	}

}