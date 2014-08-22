package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.io.BufferedReader
import java.io.FileReader
import java.lang.reflect.Modifier
import scala.util.matching.Regex
import scala.collection.mutable.ListMap
import scala.collection.mutable.LinearSeq
import sun.tools.javap;
import java.io.FileNotFoundException

object Util {

	def addEnvTypes(classes: String, envTypes: ArrayList[String], putClassLoader: ClassLoader) = {
		if (classes.size > 0) {
			classes.split(":").foreach(className => {
				val cls = Class.forName(className, true, putClassLoader)
				if (Modifier.isPublic(cls.getModifiers)) envTypes.add(className)
			})
		}
	}

	def loadFocusMethodsFromFile(fileName: String): Map[String, Int] = {
		val result = new ListMap[String, Int]()
		try {
			val r = new BufferedReader(new FileReader(fileName))

			var method = r.readLine
			while (method != null) {
				val (signature, priority) = method.split(" ") match {
					case Array(signature, priority) => (signature, priority.toInt)
					case Array(signature) => (signature, 1)
				}
				result(signature) = priority
				method = r.readLine
			}
		} catch {
			case fnfe: FileNotFoundException => println("could not find focus methods file")
		}
		result
	}

	def loadExcludedMethodsFromFile(fileName: String): List[String] = {
		var result = List[String]()
		try {
			val r = new BufferedReader(new FileReader(fileName))

			var method = r.readLine
			while (method != null) {
				result ::= method.trim
				method = r.readLine
			}
		} catch {
			case fnfe: FileNotFoundException => println("could not find excluded methods file")
		}
		result
	}

	val primitiveTypes = Set("int", "long", "boolean", "short", "float", "double", "byte", "char")
}