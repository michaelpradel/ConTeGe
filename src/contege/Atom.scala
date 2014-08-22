package contege

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import scala.Array.canBuildFrom

import GenericsReflector.getParameterTypes
import GenericsReflector.getReturnType
import GenericsReflector.getType
import contege.seqexec.reflective.Exception
import contege.seqexec.reflective.ExecutionResult
import contege.seqexec.reflective.Normal
import contege.seqexec.reflective.TimeoutRunner

/**
 * Action to be executed (e.g., a method call or a field access).
 */
abstract class Atom(val receiverType: Class[_]) {

	def execute(receiver: Object, args: Seq[Object], downcastCls: Option[Class[_]]): ExecutionResult	
	
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
	
	def adaptToReceiverType(t: String, putClassLoader: ClassLoader, config: Config): Atom
}

class MethodAtom(receiverType: Class[_], val method: Method) extends Atom(receiverType) {
    
	override def execute(receiver: Object, args: Seq[Object], downcastCls: Option[Class[_]]): ExecutionResult = {
		val result = TimeoutRunner.runWithTimeout(() => {
			try {
				if (receiver == null && !isStatic) Exception(NullAsReceiverException)
				else {
//					val r = method.invoke(receiver, args:_*)
				    val r = ReflectionHelper.methodInvoke(method, receiver, args)
					if (downcastCls.isDefined) {
						var casted = downcastCls.get.cast(r)
						Normal(casted.asInstanceOf[Object])
					} else {
						Normal(r)
					}
				}			
			} catch {
				case t: InvocationTargetException => Exception(t)
				case iae: IllegalAccessException => Exception(iae)
				case cce: ClassCastException => Exception(cce)
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
	
	override def adaptToReceiverType(t: String, putClassLoader: ClassLoader, config: Config) = {
	    val newCls = Class.forName(t, true, putClassLoader)
	    val newMethod = newCls.getMethods.find(m => m.getName == method.getName && m.getParameterTypes.map(_.getName).toList == method.getParameterTypes.map(_.getName).toList).get
	    val result = new MethodAtom(newCls, newMethod)
	    assert(paramTypes == result.paramTypes && returnType == result.returnType, "\nOld method: "+method+"\nNew method: "+newMethod)
	    result
	}
}

class ConstructorAtom(receiverType: Class[_], val constr: Constructor[_]) extends Atom(receiverType) {
	override def execute(receiver: Object, args: Seq[Object], downcastCls: Option[Class[_]]): ExecutionResult = {
		assert(receiver == null)
		val result = TimeoutRunner.runWithTimeout(() => {
			try {
//				val r = constr.newInstance(args:_*)
			    val r = ReflectionHelper.constructorNewInstance(constr, args)
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
	
	override def adaptToReceiverType(t: String, putClassLoader: ClassLoader, config: Config) = {
	    val subclassTesterConfig = config.asInstanceOf[SubclassTesterConfig]
	    val currentParamTypes = constr.getParameterTypes.map(_.getName).mkString("(",",",")")
	    val expectedParamTypes = subclassTesterConfig.constructorMap.destParams(currentParamTypes)
	    val newCls = Class.forName(t, true, putClassLoader)
	    val newConstr = newCls.getConstructors.find(c => c.getParameterTypes.map(_.getName).mkString("(",",",")") == expectedParamTypes).get
	    val result = new ConstructorAtom(newCls, newConstr)
	    result
	}
}

class FieldGetterAtom(receiverType: Class[_], val field: Field) extends Atom(receiverType) {
	override def execute(receiver: Object, args: Seq[Object], downcastCls: Option[Class[_]]): ExecutionResult = {
		try {
			if (receiver == null && !isStatic) return Exception(NullAsReceiverException)
//			val r = field.get(receiver)
			val r = ReflectionHelper.fieldGet(field, receiver)
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
	
	override def adaptToReceiverType(t: String, putClassLoader: ClassLoader, config: Config) = throw new IllegalStateException("fields with the same name hide each other in Java; shouldn't try to adapt field accesses to another receiver type")
}

object NullAsReceiverException extends RuntimeException
