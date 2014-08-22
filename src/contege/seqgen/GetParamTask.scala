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
 * Finds a variable of the given type.
 * If necessary, appends calls to the sequence to create such a variable.
 * Possibly uses some variable already in the given sequence. 
 */
class GetParamTask[CallSequence <: AbstractCallSequence[CallSequence]](seqBefore: CallSequence, typ: String,
         nullAllowed: Boolean, global: GlobalState)
extends Task[CallSequence](global) {
	
	var param: Option[Variable] = None
	
	private val maxRecursion = 50 // getting one param may require getting another; to avoid infinite recursion/very long computation, stop at some point 
	private var currentRecursion = 0 
	
	override def run = {
	    global.stats.paramTasksStarted.add("GetParamTask for "+typ)
		val ret = super.run
		if (!ret.isDefined) global.stats.paramTasksFailed.add("GetParamTask for "+typ) 
		ret		
	}
	
	def computeSequenceCandidate = {
		val newSequence = seqBefore.copy
		
		param = findVarOfType(typ, newSequence, nullAllowed)
		if (param.isDefined) {
		    Some(newSequence)
		} else {
		    None
		}
	}
	
	private def findVarOfType(typ: String, sequence: CallSequence, nullAllowed: Boolean): Option[Variable] = {
		if (currentRecursion > maxRecursion) {
			return None
		}
		currentRecursion += 1
		
		if (sequence.types.contains(typ) && global.random.nextBool) { // reuse existing var of this type
			val vars = sequence.varsOfType(typ)
		    val selectedVar = vars(global.random.nextInt(vars.size))
			return Some(selectedVar)
		} else if (!global.typeProvider.primitiveProvider.isNonRefType(typ) && nullAllowed && global.random.nextBool) {
		    return Some(NullConstant) // occasionally, use null (reduce probability?) 
		} else {
			if (global.typeProvider.primitiveProvider.isPrimitiveOrWrapper(typ)) {
				return Some(new Constant(global.typeProvider.primitiveProvider.next(typ)))
			} else { // append calls to the sequence to create a new var of this type
				var atomOption = global.typeProvider.atomGivingType(typ)
				var downcast = false
				if (!atomOption.isDefined) {
					if (nullAllowed && global.random.nextBool) {
						global.stats.nullParams.add(typ)
						return Some(NullConstant)
					} else {
					    // try to call a method where we downcast the return value
						val atomWithDowncastOption = global.typeProvider.atomGivingTypeWithDowncast(typ)
						if (atomWithDowncastOption.isDefined) {
						    atomOption = atomWithDowncastOption
						    downcast = true
						} else {
						    return None
						}
					}				
				}
				val atom = atomOption.get
				
				val receiver = if (atom.isStatic || atom.isConstructor) None 
				               else {
									// recursively try to find a variable we can use as receiver
									findVarOfType(atom.declaringType, sequence, false) match {
										case Some(r) => {
										    // if the receiver is the OUT, we should only use CUT methods (only important for subclass testing)
										    if (seqBefore.getCutVariable != null && seqBefore.getCutVariable == r && !global.typeProvider.cutMethods.contains(atom)) {
										        return None
										    }
										    Some(r)
										}
										case None => return None  // cannot find any receiver, stop searching this path
								    }
							   }
				
				val args = new ArrayList[Variable]()
				atom.paramTypes.foreach(t => {
					val arg = findVarOfType(t, sequence, true) match {
						case Some(a) => {
							args.add(a)
						}
						case None => return None // cannot find any argument, stop searching this path
					}
				})
				
				assert(atom.returnType.isDefined)
				val retVal = Some(new ObjectVariable)

				var downcastType = if (downcast) Some(typ) else None
				sequence.appendCall(atom, receiver, args, retVal, downcastType)		
		
				return retVal
			}
		}	
	}
	
}