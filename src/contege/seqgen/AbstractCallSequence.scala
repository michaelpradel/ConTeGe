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
import contege.SequencePrinter
import contege.Stats
import contege.Config
import contege.GlobalState

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
		assert(allInputVarsDefined(call), "Type to vars:\n"+type2Vars)
		calls.add(call)
		if (retVal.isDefined) {
			// remember which types this call produces

		    // use Super type if the ret val is the CUT var 
		    val typ = atom.returnType.get
		    
			assert(typ != null && typ != "void")
			val retVariable = retVal.get
			global.typeProvider.allSuperTypesAndItself(typ).foreach(t => {
				type2Vars.getOrElseUpdate(t, new ArrayList[Variable]).add(retVariable)
			})

			// create a fresh id (e.g. var5) for the return value
			id(retVariable)
		}
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
					global.stats.failedSequenceExecutions.incr
				    return Some(e)
				}
			}
		})
		return None	
	}
	
	def execute: Option[Throwable] = execute(Map[Variable, Object]())

	private val var2Id = Map[Variable, String]()
	private var nextId = 0
	def id(v: Variable) = {
		assert(v != null)
		if (v == NullConstant) "null"
		else var2Id.get(v) match {
			case Some(id) => id
			case None => {
				val newId = "var"+nextId
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
			sb.append(SequencePrinter.callToString(call, id))
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
				assert(varDefined(call.args(i), call.atom.paramTypes(i)), "undefined var at argument position "+i+" -- paramType="+call.atom.paramTypes(i)+" -- arg="+call.args(i))
			}
		} catch {
			case a: AssertionError => {
				println("Existing sequence:")
				if (this.isInstanceOf[Prefix]) println(toString)
				else if (this.isInstanceOf[Suffix]) println(this.asInstanceOf[Suffix].prefix.toString+"*****\n"+toString)
				println("Problem appending call "+SequencePrinter.callToString(call, id))
				println("Type -> Vars:")
				for ((typ,vars)<-type2Vars) {
					println("   "+typ+" --> "+vars.map(v => id(v)))
				}
				println("Ids:")
				for ((v,i) <- var2Id) {
				    println("   "+v+" --> "+i)
				}
				println("Next id: "+nextId)
				
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
		for((typ, vars) <- type2Vars) {
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
	assert(atom.nbParams == args.size, "\n"+this)
	
	override def toString = {
		"Call to "+atom+"   receiver="+receiver+"  args="+args+"  retVal="+retVal
	}
}

