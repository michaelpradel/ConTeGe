package contege.seqgen

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import contege.FieldGetterAtom
import contege.Stats
import contege.Config
import contege.GlobalState

class Suffix(val prefix: Prefix, global: GlobalState) extends AbstractCallSequence[Suffix](global) {
	
	continueWithIdFrom(prefix)
	
	override def copy = {
		val result = new Suffix(prefix, global)
		calls.foreach(call => result.appendCall(call.atom, call.receiver, call.args, call.retVal))
    	result
	}

	override def types = {
		if (global.config.shareOnlyCUTObject) {
			val result = Set[String]()
			result.addAll(super.types)
			result.add(global.config.cut)
			result
		} else prefix.types.union(super.types)
	}
	
	override def varsOfType(typ: String) = {
		val fromPrefix = if (global.config.shareOnlyCUTObject) {
							val r = new ArrayList[Variable]				
							if (typ == global.config.cut) r.add(prefix.getCutVariable) 
							r
						 } else if (prefix.types.contains(typ)) prefix.varsOfType(typ)
		                 else new ArrayList[Variable]					
		val fromHere = if (super.types.contains(typ)) super.varsOfType(typ) else new ArrayList
		val both = new ArrayList[Variable]()
		both.addAll(fromPrefix)
		both.addAll(fromHere)
		both
	}
	
	override def id(v: Variable) = {
		if (prefix.hasId(v)) prefix.id(v)
		else super.id(v)
	}
	
	override def hasId(v: Variable) = {
		prefix.hasId(v) || super.hasId(v)
	}
		
	override def varDefined(variable: Variable): Boolean = {
		super.varDefined(variable) || prefix.varDefined(variable)
	}
	
	override def varDefined(variable: Variable, typ: String): Boolean = {
		super.varDefined(variable, typ) || prefix.varDefined(variable, typ)
	}
	
	override def equivalentTo(other: AbstractCallSequence[_]): Boolean = {
		if (!other.isInstanceOf[Suffix]) return false
		val that = other.asInstanceOf[Suffix]		
		this.prefix.equivalentTo(that.prefix) && this.toString == that.toString
	}

	override def getCutVariable = prefix.getCutVariable
	
}