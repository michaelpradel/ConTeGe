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
	global: GlobalState)
	extends Task[CallSequence](global) {

	var param: Option[Variable] = None

	var searchHistory: ArrayList[String] = new ArrayList[String]()

	private val maxRecursion = 50 // getting one param may require getting another; to avoid infinite recursion/very long computation, stop at some point 
	private var currentRecursion = 0

	override def run = {
		global.stats.paramTasksStarted.add("GetParamTask for " + typ)
		val ret = super.run
		if (!ret.isDefined) {
			global.stats.paramTasksFailed.add("GetParamTask for " + typ)
		}
		global.debug("search history:", 3)
		searchHistory.foreach(entry => global.debug(entry, 3))
		ret
	}

	def computeSequenceCandidate = {
		val newSequence = seqBefore.copy

		global.debug("search var for typ: " + typ, 3)
		param = findVarOfType(typ, newSequence, true, null)
		global.debug("param: " + param, 3)
		global.debug("paramIsDefined: " + param.isDefined.toString(), 3)
		if (param.isDefined) Some(newSequence)
		else None
	}

	private def findVarOfType(typ: String, sequence: CallSequence, nullAllowed: Boolean, currentAtom: Atom): Option[Variable] = {
		val indent = " " * currentRecursion
		searchHistory.add(indent + " --- searched for: " + typ + " recursion level: " + currentRecursion)
		if (currentRecursion > maxRecursion) {
			return None
		}
		currentRecursion += 1

		if (sequence.types.contains(typ) && global.random.nextBool) { // reuse existing var of this type
			val vars = sequence.varsOfType(typ)
			val selectedVar = vars(global.random.nextInt(vars.size))
			searchHistory.add(indent + "   reusing var for typ: " + typ)
			return Some(selectedVar)
		} else {
			searchHistory.add(indent + "   creating new Var for typ: " + typ)
			if (global.typeProvider.primitiveProvider.isPrimitiveOrWrapper(typ)) {
				searchHistory.add(indent + "   if")
				return Some(new Constant(global.typeProvider.primitiveProvider.next(typ)))
			} else { // append calls to the sequence to create a new var of this type
				searchHistory.add(indent + "   else")
				val atomOption = global.typeProvider.atomGivingType(typ)
				global.debug("typ: " + typ, 3)
				global.debug("atomOption: " + atomOption, 3)
				if (!atomOption.isDefined) {
					if (nullAllowed) {
						global.stats.nullParams.add(typ)
						return Some(NullConstant)
					} else {
						global.stats.noParamFound.add(typ)
						return None
					}
				}
				val atom = atomOption.get

				if (currentAtom == atom && nullAllowed) {
					return Some(NullConstant)
				}

				searchHistory.add(indent + "   using: " + atom)

				val receiver = if (atom.isStatic || atom.isConstructor) None
				else {
					// recursively try to find a variable we can use as receiver
					findVarOfType(atom.declaringType, sequence, false, atom) match {
						case Some(r) => {
							// if the receiver is the OUT, we should only use CUT methods (only important for subclass testing)
							if (seqBefore.getCutVariable != null && seqBefore.getCutVariable == r && !global.typeProvider.cutMethods.contains(atom)) {
								return None
							}
							Some(r)
						}
						case None => return None // cannot find any receiver, stop searching this path
					}
				}

				global.debug("receiver: " + receiver, 3)
				val args = new ArrayList[Variable]()

				atom.paramTypes.foreach(t => {
					val arg = findVarOfType(t, sequence, true, atom) match {
						case Some(a) => {
							args.add(a)
						}
						case None => return None // cannot find any argument, stop searching this path
					}
				})

				global.debug("returnType: " + atom.returnType, 3)

				assert(atom.returnType.isDefined)
				val retVal = Some(new ObjectVariable)

				global.debug(" -- trying to append call", 3)
				sequence.appendCall(atom, receiver, args, retVal)
				global.debug(" -- call appended", 3)
				global.debug("sequence: " + sequence, 3)

				return retVal
			}
		}
	}

}