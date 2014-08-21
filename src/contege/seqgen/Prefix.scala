package contege.seqgen

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import contege.FieldGetterAtom
import contege.Stats
import contege.Config
import contege.GlobalState

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
	
}