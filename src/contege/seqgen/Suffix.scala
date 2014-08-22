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

	def copyWithOracleClassReceiver(oracleClass: String, adaptedPrefix: Prefix) = {
		val result = new Suffix(adaptedPrefix, global)
		addCallsAndAdaptType(oracleClass, result, prefix.getCutVariable)
		result
	}

	override def getCutVariable = prefix.getCutVariable

	def adaptToNewClassVersion(prefix: Prefix, var2var: ListMap[Option[Variable], Option[Variable]], classLoader: CustomClassLoader): (Suffix, Boolean) = {
		val newSuffix = new Suffix(prefix, global)
		var success = true
		
		calls.foreach(call => {
			try {
				if (call.atom.isConstructor) {
					val (atom, receiver, args, retVal) = adaptConstructor(call, classLoader, var2var)
					newSuffix.appendCall(atom, None, args, retVal)
				} else if (call.atom.isMethod || call.atom.isField) {
					val (atom, receiver, args, retVal) = adaptMethodOrFieldAccess(call, classLoader, var2var)
					newSuffix.appendCall(atom, receiver, args, retVal)
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

		return (newSuffix, success)
	}

	def containsAnyFocusMethod(): Boolean = {
		var result = false
		global.focusMethods match {
			case (Some(map)) => {
				calls.foreach(call => {
					if (map.keys.contains(call.atom.signature)) {
						result = true
					}
				})
			}
			case None => //ignore
		}
		return result
	}

}