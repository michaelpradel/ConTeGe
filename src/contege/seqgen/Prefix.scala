package contege.seqgen

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import contege.FieldGetterAtom
import contege.Stats
import contege.Config
import contege.GlobalState
import scala.collection.mutable.ListMap
import contege.CustomClassLoader

class Prefix(global: GlobalState) extends AbstractCallSequence[Prefix](global) {

	private var cutVariable: ObjectVariable = _
	
	/**
	 * Set CUT variable to the return value of the last call in the sequence
	 */
	def fixCutVariable = {
		val lastCall = calls.last
		assert(lastCall.retVal.isDefined, this)
		assert(lastCall.retVal.get.isInstanceOf[ObjectVariable], this)
		cutVariable = lastCall.retVal.get.asInstanceOf[ObjectVariable]
	}
	
	override def getCutVariable = {
		cutVariable
	}
	
    override def copy = {
    	val result = new Prefix(global)
    	var cutVarAssigned = false
    	calls.foreach(call => {
    	    val producesCutVar = if (cutVarAssigned) false
    	    else {
    	        if (call.retVal.isDefined && call.retVal.get == getCutVariable) {
    	            cutVarAssigned = true
    	        	true
    	        } 
    	        else false
    	    }
    	    
    		result.appendCall(call.atom, call.receiver, call.args, call.retVal, producesCutVar)
    	})
    	result.cutVariable = cutVariable
    	result
    }
	
	override def equivalentTo(other: AbstractCallSequence[_]): Boolean = {
		if (!other.isInstanceOf[Prefix]) return false
		val that = other.asInstanceOf[Prefix] 
		if (this.cutVariable == null || that.cutVariable == null) return false
		this.toString == that.toString && this.id(this.cutVariable) == that.id(that.cutVariable)
	}
	
	def copyWithOracleClassReceiver(oracleClass: String) = {
	    val result = new Prefix(global)
    	addCallsAndAdaptType(oracleClass, result, cutVariable)
    	result.cutVariable = cutVariable
    	result
	}
	
	override def endsWithCUTInstantiation = {
	    if (calls.last.atom.isConstructor && calls.last.retVal.get == cutVariable) true
	    else false
	}
	
	override def endsWithCUTCall = {
	    calls.last.receiver match {
	        case Some(rec) => (rec == cutVariable)
	        case None => false
	    }
	}
	
	def adaptToNewClassVersion(classLoader: CustomClassLoader): (Prefix, ListMap[Option[Variable], Option[Variable]], Boolean) = {
		val newPrefix = new Prefix(global)
		var var2var = ListMap[Option[Variable], Option[Variable]]()
		var success = true
		
		calls.foreach(call => {
			try {
				if (call.atom.isConstructor) {
					val (atom, receiver, args, retVal) = adaptConstructor(call, classLoader, var2var)
					newPrefix.appendCall(atom, receiver, args, retVal)
				} else if (call.atom.isMethod || call.atom.isField) {
					val (atom, receiver, args, retVal) = adaptMethodOrFieldAccess(call, classLoader, var2var)
					newPrefix.appendCall(atom, receiver, args, retVal)
				} else {
				  throw new Exception("could not adapt call: " + call.toString)
				}
			} catch {
				case a: AssertionError  => {
					success = false
					println(" >>> WARNING: could not append call: " + call.toString)
				}
			}
    	})  
    	
    	return (newPrefix, var2var, success)
	}
}