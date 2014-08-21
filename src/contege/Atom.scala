package contege

import java.lang.reflect._
import scala.collection.JavaConversions._
import contege.seqexec._
import contege.seqexec.reflective._
import GenericsReflector._

/**
 * Action to be executed (e.g., a method call or a field access).
 */
abstract class Atom(val receiverType: Class[_]) {

	def execute(receiver: Object, args: Seq[Object]): ExecutionResult	
	
	def nbParams: Int
	
	def paramTypes: Seq[String]
	
	def returnType: Option[String]
	
	def declaringType: String
	
	def methodName: String
	
	def isStatic: Boolean

	def isConstructor: Boolean
	
	def isMethod: Boolean
	
	def isField: Boolean
	
	def signature: String
	
	override def toString() = signature
	
}

class MethodAtom(receiverType: Class[_], val method: Method) extends Atom(receiverType) {
	override def execute(receiver: Object, args: Seq[Object]): ExecutionResult = {
		val result = TimeoutRunner.runWithTimeout(() => {
			try {
				if (receiver == null && !isStatic) Exception(NullAsReceiverException)
				else {
					val r = method.invoke(receiver, args:_*)
					Normal(r)
				}			
			} catch {
				case t: InvocationTargetException => Exception(t)
				case iae: IllegalAccessException => Exception(iae)
			}		
		}, method.toString)			
		result
	}
	
	override def nbParams = method.getParameterTypes.size
	
	override def paramTypes = getParameterTypes(method, receiverType)
	
	override def returnType: Option[String] = {
		val t = getReturnType(method, receiverType)
		if (t == "void") None else Some(t)
	}
	
	override def declaringType = method.getDeclaringClass.getName
		
	override def methodName = method.getName
	
	override def signature = receiverType.getName+"."+method.getName+paramTypes.mkString("(",",",")")
	
	override def isStatic = Modifier.isStatic(method.getModifiers)
	
	override def isConstructor = false
	
	override def isMethod = true
	
	override def isField = false
	
}

class ConstructorAtom(receiverType: Class[_], val constr: Constructor[_]) extends Atom(receiverType) {
	override def execute(receiver: Object, args: Seq[Object]): ExecutionResult = {
		assert(receiver == null)
		val result = TimeoutRunner.runWithTimeout(() => {
			try {
				val r = constr.newInstance(args:_*)
				Normal(r.asInstanceOf[Object])
			} catch {
				case t: InvocationTargetException => Exception(t)
				case iae: IllegalAccessException => Exception(iae)
			}		
		}, constr.toString)			
		result
	}
	
	override def nbParams = constr.getParameterTypes.size
	
	override def paramTypes = getParameterTypes(constr, receiverType)
	
	override def returnType = Some(receiverType.getName)
	
	override def declaringType = constr.getDeclaringClass.getName
	
	override def methodName = ""
		
	override def isStatic = false
	
	override def isConstructor = true
	
	override def isMethod = false

	override def isField = false
	
	override def signature = receiverType.getName+paramTypes.mkString("(",",",")")
}

class FieldGetterAtom(receiverType: Class[_], val field: Field) extends Atom(receiverType) {
	override def execute(receiver: Object, args: Seq[Object]): ExecutionResult = {
		try {
			if (receiver == null && !isStatic) return Exception(NullAsReceiverException)
			val r = field.get(receiver)
			return Normal(r)
		} catch {
			case t: InvocationTargetException => Exception(t)
			case iae: IllegalAccessException => Exception(iae)
		}		
	}
	
	override def nbParams = 0
	
	override def paramTypes = List()
	
	override def returnType: Option[String] = {
		val t = getType(field, receiverType)
		Some(t)
	}
	
	override def declaringType = field.getDeclaringClass.getName
		
	override def methodName = field.getName
	
	override def signature = receiverType.getName+"."+field.getName
	
	override def isStatic = Modifier.isStatic(field.getModifiers)
	
	override def isConstructor = false
	
	override def isMethod = false
	
	override def isField = true
	
}

object NullAsReceiverException extends RuntimeException
