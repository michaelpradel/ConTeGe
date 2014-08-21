package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.io.BufferedReader
import java.io.FileReader
import java.lang.reflect.Modifier

object Util {

	def addEnvTypes(classesFileName: String, envTypes: ArrayList[String], putClassLoader: ClassLoader) = {
		val r = new BufferedReader(new FileReader(classesFileName))
		var className = r.readLine
		while (className != null) {
			val cls = Class.forName(className, true, putClassLoader)
			if (Modifier.isPublic(cls.getModifiers)) envTypes.add(className)
			className = r.readLine
		}		
		r.close
	}
	
	val primitiveTypes = Set("int", "long", "boolean", "short", "float", "double", "byte", "char")
}