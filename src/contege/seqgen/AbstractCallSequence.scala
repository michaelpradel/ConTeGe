package contege.seqgen

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import contege.Atom
import contege.ConstructorAtom
import contege.MethodAtom
import contege.seqexec._
import contege.seqexec.reflective._
import contege.SubclassTesterConfig
import contege.SequencePrinter
import contege.Stats
import contege.Config
import contege.GlobalState
import contege.CustomClassLoader
import scala.collection.mutable.ListMap

/**
 * A sequence of calls, e.g. a prefix or a suffix.
 */
abstract class AbstractCallSequence[MyType](global: GlobalState) {

	protected val calls = new ArrayList[Call]()

	private val type2Vars = Map[String, ArrayList[Variable]]() // all variables returned by calls in this sequence

	def appendCall(atom: Atom, receiver: Option[Variable], args: Seq[Variable], retVal: Option[Variable]): Unit = {
		appendCall(atom, receiver, args, retVal, false)
	}

	def appendCall(atom: Atom, receiver: Option[Variable], args: Seq[Variable], retVal: Option[Variable], producesCutVar: Boolean): Unit = {
		val call = new Call(atom, receiver, args, retVal)
		assert(allInputVarsDefined(call), "Type to vars:\n" + type2Vars)
		calls.add(call)
		if (retVal.isDefined) {
			// remember which types this call produces

			// use Super type if the ret val is the CUT var 
			val typ = if (global.config.isInstanceOf[SubclassTesterConfig] &&
				(producesCutVar || retVal == getCutVariable)) global.config.asInstanceOf[SubclassTesterConfig].oracleClass
			else atom.returnType.get

			assert(typ != null && typ != "void")
			val retVariable = retVal.get
			global.typeProvider.allSuperTypesAndItself(typ).foreach(t => {
				type2Vars.getOrElseUpdate(t, new ArrayList[Variable]).add(retVariable)
			})

			// create a fresh id (e.g. var5) for the return value
			id(retVariable)
		}
	}

	def adaptConstructor(call: Call, classLoader: CustomClassLoader, var2var: ListMap[Option[Variable], Option[Variable]]): (Atom, Option[Variable], Seq[Variable], Option[Variable]) = {
		val newAtom = call.atom.adaptToNewClassVersion(classLoader)
		val retVal = newAtom.returnType match {
			case Some(t) => Some(new ObjectVariable)
			case None => None
		}
		var2var(call.retVal) = retVal
		val newArgs = new ArrayList[Variable]()
		call.args.foreach(arg => {
			if (var2var.keys.contains(Some(arg))) {
				newArgs.add(var2var(Some(arg)).get)
			} else {
				val newVar = arg match {
					case c: Constant => new Constant(c.value)
					case _ => NullConstant
				}
				var2var(Some(arg)) = Some(newVar)
				newArgs.add(newVar)
			}
		})
		return (newAtom, None, newArgs, retVal)
	}

	def adaptMethodOrFieldAccess(call: Call, classLoader: CustomClassLoader, var2var: ListMap[Option[Variable], Option[Variable]]): (Atom, Option[Variable], Seq[Variable], Option[Variable]) = {
		val receiver = call.receiver match {
			case None => None
			case _ => var2var(call.receiver)
		}
		val newAtom = call.atom.adaptToNewClassVersion(classLoader)
		val retVal = newAtom.returnType match {
			case Some(t) => Some(new ObjectVariable)
			case None => None
		}
		var2var(call.retVal) = retVal
		val newArgs = new ArrayList[Variable]()
		call.args.foreach(arg => {
			if (var2var.keys.contains(Some(arg))) {
				newArgs.add(var2var(Some(arg)).get)
			} else {
				val newVar = arg match {
					case c: Constant => new Constant(c.value)
					case _ => NullConstant
				}
				var2var(Some(arg)) = Some(newVar)
				newArgs.add(newVar)
			}
		})
		return (newAtom, receiver, newArgs, retVal)
	}

	def callIterator = calls.iterator

	def length = calls.length

	def copy: MyType

	def types = type2Vars.keySet

	def varsOfType(typ: String) = type2Vars(typ)

	def execute(var2Object: Map[Variable, Object]): Option[Throwable] = {
		def obj(v: Variable): Object = v match {
			case c: Constant => c.value
			case NullConstant => null
			case _ => var2Object(v)
		}

		calls.foreach(call => {
			val receiverObj = call.receiver match {
				case Some(recVar) => obj(recVar)
				case None => null
			}
			val argObjs = call.args.map(argVar => obj(argVar))
			val result = call.atom.execute(receiverObj, argObjs)
			result match {
				case Normal(retVal) => call.retVal match {
					case Some(retValVar) => var2Object.put(retValVar, retVal)
					case None => assert(retVal == null, "Call should return void when call.retVal is undefined")
				}
				case Exception(e) => {
					global.debug(" -- call atom     : " + call.atom, 2)
					if (e.toString == "contege.NullAsReceiverException$") {
						global.debug("AbstractCallSequence: error:" + e.toString, 2)
					} else {
						try {
							global.debug(" -- exception     : " + e.toString, 2)
							global.debug(" -- cause         : " + e.getCause, 2)
							global.debug(" -- receiverObject: " + receiverObj, 2)
						} catch {
							case ex1:IllegalArgumentException => {
								global.debug(" -- there was an exception when accessing cause of the error", 2)
								return Some(ex1)
							}
							case ex2 => {
								global.debug(" -- there was an exception when accessing the receiver object", 2)
								return Some(ex2)
							}
						}
					}

					global.debug(" -- arguments     : ", 2)
					argObjs.foreach(arg => (global.debug("   - arg: " + arg, 2)))
					global.stats.failedSequenceExecutions.incr

					return Some(e)
				}
			}
		})
		return None
	}

	def executeForPerformance(var2Object: Map[Variable, Object]): Boolean = {
		def obj(v: Variable): Object = v match {
			case c: Constant => c.value
			case NullConstant => null
			case _ => var2Object(v)
		}

		var valid = true
		calls.foreach(call => {
			val receiverObj = call.receiver match {
				case Some(recVar) => obj(recVar)
				case None => null
			}
			val argObjs = call.args.map(argVar => obj(argVar))
			try {
				val r = call.atom.pureExecute(receiverObj, argObjs)
				r match {
					case Normal(retVal) => {
						call.retVal match {
							case Some(retValVar) => var2Object.put(retValVar, retVal)
							case None => assert(retVal == null, "Call should return void when call.retVal is undefined")
						}
					}
					case Exception(e) => {
						global.debug("Exception when calling: " + call.atom.toString, 1)
						global.debug(e.getCause().toString(), 1)
						valid = false
					}
				}
			} catch {
				case e => {
					global.debug("Exception when calling: " + call.atom.toString, 1)
					global.debug(e.getCause().toString(), 1)
					valid = false
				}
			}
		})

		return valid
	}

	def execute: Option[Throwable] = execute(Map[Variable, Object]())

	def executeWithOutputVector(var2Object: Map[Variable, Object], outputVector: OutputVector): (Option[Throwable], OutputVector) = {
		def obj(v: Variable): Object = v match {
			case c: Constant => c.value
			case NullConstant => null
			case _ => var2Object(v)
		}

		calls.foreach(call => {
			val receiverObj = call.receiver match {
				case Some(recVar) => obj(recVar)
				case None => null
			}
			val argObjs = call.args.map(argVar => obj(argVar))
			val result = call.atom.execute(receiverObj, argObjs)
			result match {
				case Normal(retVal) => {
					call.retVal match {
						case Some(retValVar) => {
							var2Object.put(retValVar, retVal)

							if (OutputVector.methodsToIgnore.contains(call.atom.methodName)) {
								outputVector.add(OutputVector.IgnoreString)
							} else {
								outputVector.add(retVal)
							}
						}
						case None => assert(retVal == null, "Call should return void when call.retVal is undefined")
					}
				}
				case Exception(e) => {
					global.stats.failedSequenceExecutions.incr
					global.debug(" -- Exception while executing: " + call.atom.toString, 1)
					return (Some(e), outputVector)
				}
			}
		})
		return (None, outputVector)
	}

	def executeWithOutputVector: (Option[Throwable], OutputVector) = executeWithOutputVector(Map[Variable, Object](), new OutputVector)

	private val var2Id = Map[Variable, String]()
	private var nextId = 0
	def id(v: Variable) = {
		assert(v != null)
		if (v == NullConstant) "null"
		else var2Id.get(v) match {
			case Some(id) => id
			case None => {
				val newId = "var" + nextId
				nextId += 1
				var2Id.put(v, newId)
				newId
			}
		}
	}
	def hasId(v: Variable) = (v == NullConstant) || (var2Id.contains(v))

	protected def continueWithIdFrom(otherCallSequence: AbstractCallSequence[_]) = {
		nextId = otherCallSequence.nextId
	}

	override def toString = {
		val sb = new StringBuilder
		calls.foreach(call => {
			sb.append(SequencePrinter.callToString(call, id, SequencePrinter.NoOutput))
			sb.append("\n")
		})
		sb.toString
	}

	def toStringWithOutputVector = {
		val sb = new StringBuilder
		calls.foreach(call => {
			// check if result should be added to output vector
			val outputConfig = if (!call.atom.returnType.isDefined) SequencePrinter.NoOutput
			else if (OutputVector.methodsToIgnore.contains(call.atom.methodName)) SequencePrinter.FillOutputVectorWithIgnore
			else SequencePrinter.FillOutputVector

			sb.append(SequencePrinter.callToString(call, id, outputConfig))
			sb.append("\n")
		})
		sb.toString
	}

	/**
	 * Checks if the variables used as receiver and arguments are defined in this sequence.
	 */
	def allInputVarsDefined(call: Call): Boolean = {
		try {
			if (call.receiver.isDefined) {
				assert(varDefined(call.receiver.get))
			}

			for (i <- 0 to call.args.size - 1) {
				assert(varDefined(call.args(i), call.atom.paramTypes(i)), "undefined var at argument position " + i + " -- paramType=" + call.atom.paramTypes(i) + " -- arg=" + call.args(i))
			}
		} catch {
			case a: AssertionError => {
				println("Existing sequence:")
				if (this.isInstanceOf[Prefix]) println(toString)
				else if (this.isInstanceOf[Suffix]) println(this.asInstanceOf[Suffix].prefix.toString + "*****\n" + toString)
				println("Problem appending call " + SequencePrinter.callToString(call, id, SequencePrinter.NoOutput))
				println("Type -> Vars:")
				for ((typ, vars) <- type2Vars) {
					println("   " + typ + " --> " + vars.map(v => id(v)))
				}
				println("Ids:")
				for ((v, i) <- var2Id) {
					println("   " + v + " --> " + i)
				}
				println("Next id: " + nextId)

				println(a.getMessage())
				a.printStackTrace
				return false
			}
		}
		true
	}

	/**
	 * If possible, pass type to this method (for performance).
	 * Only use this for calls of static methods on instances.
	 */
	def varDefined(variable: Variable): Boolean = {
		for ((typ, vars) <- type2Vars) {
			if (vars.contains(variable)) return true
		}
		false
	}

	def varDefined(variable: Variable, typ: String): Boolean = {
		if (variable.isInstanceOf[ObjectVariable]) {
			type2Vars.get(typ) match {
				case Some(vars) => return vars.contains(variable)
				case None => return false
			}
		} else return true
	}

	def equivalentTo(that: AbstractCallSequence[_]): Boolean

	protected def addCallsAndAdaptType(newType: String, result: AbstractCallSequence[_], cutVariable: Variable) = {
		var gotCUTInstance = false
		calls.foreach(call => {
			val assignsToCUTVar = if (gotCUTInstance) false
			else {
				if (call.retVal.isDefined && call.retVal.get == getCutVariable) {
					gotCUTInstance = true
					true
				} else false
			}

			if (cutVariable != null && call.atom.isMethod && call.receiver == Some(cutVariable)) {
				// adapt called method if receiver is the CUT variable
				val newAtom = call.atom.adaptToReceiverType(newType, global.typeProvider.classLoaderV1, global.config)
				assert(!assignsToCUTVar, "do we have methods tt assign to the CUT variable?")
				result.appendCall(newAtom, call.receiver, call.args, call.retVal, assignsToCUTVar)
			} else if (cutVariable != null && call.atom.isConstructor && call.retVal == Some(cutVariable)) {
				// adapt calls to target class constructor
				assert(call.receiver == None)
				val newAtom = call.atom.adaptToReceiverType(newType, global.typeProvider.classLoaderV1, global.config)
				val subclassTesterConfig = global.config.asInstanceOf[SubclassTesterConfig]
				val currentParamTypes = call.atom.paramTypes.mkString("(", ",", ")")
				val destParamPositions = subclassTesterConfig.constructorMap.destParamPositions(currentParamTypes)
				val newArgs = new ArrayList[Variable]
				destParamPositions.foreach(pos => newArgs.add(call.args(pos - 1)))
				result.appendCall(newAtom, call.receiver, newArgs, call.retVal, assignsToCUTVar)
			} else {
				// leave all other calls the way they are
				// - for field accesses use superclass field, because we assume that the class is used via the supertype itf
				// - same for static calls, which are statically resolved
				assert(!assignsToCUTVar, "do we have methods that assign to the CUT var?")
				result.appendCall(call.atom, call.receiver, call.args, call.retVal, assignsToCUTVar)
			}
		})
	}

	def endsWithCUTInstantiation = false

	def endsWithCUTCall = false

	def getCutVariable: Variable

	def nbNonVoidCalls = {
		calls.filter(_.atom.returnType.isDefined).size
	}

}

abstract class Variable
class ObjectVariable extends Variable
class Constant(val value: Object) extends Variable // primitive type or String
object NullConstant extends Variable // primitive type or String

class Call(val atom: Atom, val receiver: Option[Variable], val args: Seq[Variable], val retVal: Option[Variable]) {
	assert(atom.nbParams == args.size, "\n" + this)

	override def toString = {
		"Call to " + atom + "   receiver=" + receiver + "  args=" + args + "  retVal=" + retVal
	}
}

