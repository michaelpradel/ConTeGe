package contege

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import scala.Array.canBuildFrom
import scala.annotation.elidable

import GenericsReflector.getParameterTypes
import GenericsReflector.getReturnType
import GenericsReflector.getType
import annotation.elidable.ASSERTION
import contege.seqexec.reflective.Exception
import contege.seqexec.reflective.ExecutionResult
import contege.seqexec.reflective.Normal
import contege.seqexec.reflective.TimeoutRunner

/**
 * Action to be executed (e.g., a method call or a field access).
 */
abstract class Atom(val receiverType: Class[_]) {

	def execute(receiver: Object, args: Seq[Object]): (ExecutionResult)

	def pureExecute(receiver: Object, args: Seq[Object]): (ExecutionResult)

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

	def adaptToNewClassVersion(classLoader: CustomClassLoader): Atom
}

class MethodAtom(receiverType: Class[_], val method: Method) extends Atom(receiverType) {

	override def execute(receiver: Object, args: Seq[Object]): ExecutionResult = {
		val result = TimeoutRunner.runWithTimeout(() => {
			try {
				if (receiver == null && !isStatic) Exception(NullAsReceiverException)
				else {
					val r = method.invoke(receiver, args: _*)
					Normal(r)
				}
			} catch {
				case t: InvocationTargetException => Exception(t)
				case iae: IllegalAccessException => Exception(iae)
				case iae2: IllegalArgumentException => Exception(iae2)
				case e: Exception => Exception(e)
			}
		}, method.toString)
		result
	}

	override def pureExecute(receiver: Object, args: Seq[Object]): (ExecutionResult) = {
		val result = try {
			Normal(method.invoke(receiver, args: _*))
		} catch {
			case t: InvocationTargetException => Exception(t)
			case iae: IllegalAccessException => Exception(iae)
			case iae2: IllegalArgumentException => Exception(iae2)
			case e: Exception => Exception(e)		
		}
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

	override def signature = receiverType.getName + "." + method.getName + paramTypes.mkString("(", ",", ")")

	override def isStatic = Modifier.isStatic(method.getModifiers)

	override def isConstructor = false

	override def isMethod = true

	override def isField = false

	def equals(other: MethodAtom): Boolean = {
		val otherName = other.method.getName
		val myParams = method.getParameterTypes.map(_.getName).toList
		val otherParams = other.method.getParameterTypes.map(_.getName).toList
		var result = false
		if ((methodName == otherName) && (myParams == otherParams)) {
			result = true
		}
		return result
	}

	override def adaptToReceiverType(t: String, putClassLoader: ClassLoader, config: Config) = {
		val newCls = Class.forName(t, true, putClassLoader)
		val newMethod = newCls.getMethods.find(m => m.getName == method.getName && m.getParameterTypes.map(_.getName).toList == method.getParameterTypes.map(_.getName).toList).get
		val result = new MethodAtom(newCls, newMethod)
		assert(paramTypes == result.paramTypes && returnType == result.returnType, "\nOld method: " + method + "\nNew method: " + newMethod)
		result
	}

	override def equals(other: Any) = other match {
		case that: MethodAtom => {
			val otherName = that.method.getName
			val myParams = this.method.getParameterTypes.map(_.getName).toList
			val otherParams = that.method.getParameterTypes.map(_.getName).toList
			(methodName == otherName) && (myParams == otherParams)
		}
		case _ => false
	}

	override def hashCode = {
		(methodName + method.getParameterTypes().map(_.getName).toList.toString).hashCode
	}

	override def adaptToNewClassVersion(classLoader: CustomClassLoader): MethodAtom = {
		val newCls = Class.forName(declaringType, true, classLoader);
		val newMethod = newCls.getMethods.find(m => m.getName == methodName && m.getParameterTypes.map(_.getName).toList == method.getParameterTypes.map(_.getName).toList).get
		val newAtom = new MethodAtom(newCls, newMethod)
		newAtom
	}

}

class ConstructorAtom(receiverType: Class[_], val constr: Constructor[_]) extends Atom(receiverType) {

	//override def executeWithTimeout(receiver: Object, args: Seq[Object]): (ExecutionResult) = {
	override def execute(receiver: Object, args: Seq[Object]): (ExecutionResult) = {
		assert(receiver == null)
		val result = TimeoutRunner.runWithTimeout(() => {
			try {
				val r = constr.newInstance(args: _*)
				//val r = invocationHandler.invokeConstructor(receiverType, constr, args.toArray)
				Normal(r.asInstanceOf[Object])
			} catch {
				case t: InvocationTargetException => Exception(t)
				case iae: IllegalAccessException => Exception(iae)
			}
		}, constr.toString)
		(result)
	}

	override def pureExecute(receiver: Object, args: Seq[Object]): (ExecutionResult) = {
		val result = try {
			Normal(constr.newInstance(args: _*).asInstanceOf[Object])
		} catch {
			case t: InvocationTargetException => Exception(t)
			case iae: IllegalAccessException => Exception(iae)
		}
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

	override def signature = receiverType.getName + paramTypes.mkString("(", ",", ")")

	override def adaptToReceiverType(t: String, putClassLoader: ClassLoader, config: Config) = {
		val subclassTesterConfig = config.asInstanceOf[SubclassTesterConfig]
		val currentParamTypes = constr.getParameterTypes.map(_.getName).mkString("(", ",", ")")
		val expectedParamTypes = subclassTesterConfig.constructorMap.destParams(currentParamTypes)
		val newCls = Class.forName(t, true, putClassLoader)
		val newConstr = newCls.getConstructors.find(c => c.getParameterTypes.map(_.getName).mkString("(", ",", ")") == expectedParamTypes).get
		val result = new ConstructorAtom(newCls, newConstr)
		result
	}

	def equals(other: ConstructorAtom): Boolean = {
		val myParams = constr.getParameterTypes.map(_.getName).toList
		val otherParams = other.constr.getParameterTypes.map(_.getName).toList
		var result = false
		if (myParams == otherParams) {
			result = true
		}
		return result
	}

	override def equals(other: Any) = other match {
		case that: ConstructorAtom => {
			val myParams = this.constr.getParameterTypes.map(_.getName).toList
			val otherParams = that.constr.getParameterTypes.map(_.getName).toList
			myParams == otherParams
		}
		case _ => false
	}

	override def hashCode = constr.getParameterTypes().map(_.getName).toList.toString.hashCode

	override def adaptToNewClassVersion(classLoader: CustomClassLoader): ConstructorAtom = {
		val newCls = Class.forName(declaringType, true, classLoader);
		val myParams = constr.getParameterTypes.map(_.getName).toList
		val newConstr = newCls.getConstructors.find(c => c.getParameterTypes.map(_.getName).toList == myParams).get
		val newAtom = new ConstructorAtom(newCls, newConstr)
		newAtom
	}
}

class FieldGetterAtom(receiverType: Class[_], val field: Field) extends Atom(receiverType) {

	override def execute(receiver: Object, args: Seq[Object]): (ExecutionResult) = {
		assert(receiver == null)
		val result = TimeoutRunner.runWithTimeout(() => {
			try {
				val r = field.get(receiver)
				//val r = invocationHandler.invokeConstructor(receiverType, constr, args.toArray)
				Normal(r.asInstanceOf[Object])
			} catch {
				case t: InvocationTargetException => Exception(t)
				case iae: IllegalAccessException => Exception(iae)
			}
		}, field.toString)
		(result)
	}

	override def pureExecute(receiver: Object, args: Seq[Object]): (ExecutionResult) = {
		val result = try {
			Normal(field.get(receiver))
		} catch {
			case t: InvocationTargetException => Exception(t)
			case iae: IllegalAccessException => Exception(iae)
		}
		result
	}

	override def nbParams = 0

	override def paramTypes = List()

	override def returnType: Option[String] = {
		val t = getType(field, receiverType)
		Some(t)
	}

	override def declaringType = field.getDeclaringClass.getName

	override def methodName = field.getName

	override def signature = receiverType.getName + "." + field.getName

	override def isStatic = Modifier.isStatic(field.getModifiers)

	override def isConstructor = false

	override def isMethod = false

	override def isField = true

	override def adaptToReceiverType(t: String, putClassLoader: ClassLoader, config: Config) = throw new IllegalStateException("fields with the same name hide each other in Java; shouldn't try to adapt field accesses to another receiver type")

	override def equals(other: Any) = other match {
		case that: FieldGetterAtom => that.field.getName == this.field.getName
		case _ => false
	}

	override def hashCode = field.getName.hashCode

	override def adaptToNewClassVersion(classLoader: CustomClassLoader): FieldGetterAtom = {
		val newCls = Class.forName(declaringType, true, classLoader);
		val newField = newCls.getField(field.getName)
		val newAtom = new FieldGetterAtom(newCls, newField)
		newAtom
	}
}

object NullAsReceiverException extends RuntimeException

object UndefinedException extends RuntimeException
