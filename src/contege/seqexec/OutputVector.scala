package contege.seqexec

import java.util.ArrayList
import javamodel.util.TypeUtil
import scala.collection.mutable.Set
import scala.collection.JavaConversions._

/**
 * Represents the output (i.e., return values of method calls) of an execution. 
 */
class OutputVector {

    // encoding:
    // - null --> OutputVector.NullValue
    // - primitive values and String --> add directly (use autoboxing)
    // - ref values --> store a String, e.g. "REF:qualified.NameOfClass"
    private val outputs = new ArrayList[Object]
    
    // only take Objects/AnyRefs; enforce boxing at call site
    def add(o: Object) = {
        synchronized {
	        if (o == null) outputs.add(OutputVector.NullValue)
	        else {
	            val typ = o.getClass.getName
	            if (OutputVector.primTypes.contains(typ) || typ == "java.lang.String") outputs.add(o)
	            else outputs.add("REF")
	        }
        }
    }
    
    private def addRefValue(stringRepresentation: String) = {
        outputs.add(stringRepresentation)
    }
    
    override def toString = {
        val sb = new StringBuilder
        outputs.foreach(o => {
            if (o == OutputVector.NullValue) sb.append("null")
            else {
                val typ = o.getClass.getName
                if (OutputVector.primTypes.contains(typ)) {
                    sb.append("PRIM@@@"+typ+"@@@"+o.toString)
                } else {
                    sb.append(o)
                }
            }
            sb.append("\n")
        })
        if (sb.size > 0) sb.deleteCharAt(sb.size - 1) // delete last new line
        sb.toString
    }
    
    override def hashCode = {
        outputs.hashCode
    }
    
    override def equals(other: Any): Boolean = {
        if (!(other.isInstanceOf[OutputVector])) return false
        val that = other.asInstanceOf[OutputVector]  
        
        return this.outputs == that.outputs // Java's equals is sufficient here: have only NullValue, boxed primitives, and Strings
    }
    
    def lastNOutputsOnly(n: Int) = {
        assert(outputs.size >= n)
        val result = new OutputVector
        outputs.slice(outputs.size - n, outputs.size).foreach(o => {
        	result.outputs.add(o)
        })
        result
    }
    
    def last = {
        outputs.last
    }

    def sameSetAs(that: OutputVector) = {
        this.outputs.toSet == that.outputs.toSet
    }
    
}

object OutputVector {
    object NullValue
    val IgnoreString = "...ignore..."
    
    val primTypes = TypeUtil.boxed2Primitive.keySet
    
    val methodsToIgnore = Set("toString", "hashCode")
    
    def fromString(s: String): OutputVector = {
        val result = new OutputVector
        val lines = s.split("\n")
        lines.foreach(l => {
            if (l == "null") result.add(null)
            else {
                if (l == "REF") {
                    result.add(l)                    
                } else if (l.startsWith("PRIM@@@")) {
                    val splitted = l.split("@@@")
                    assert(splitted.size == 3)
                    val typ = splitted(1)
                    val valAsString = splitted(2)
                    if (typ == "java.lang.Byte") result.add(java.lang.Byte.valueOf(valAsString))
                    else if (typ == "java.lang.Short") result.add(java.lang.Short.valueOf(valAsString))
                    else if (typ == "java.lang.Integer") result.add(java.lang.Integer.valueOf(valAsString))
                    else if (typ == "java.lang.Long") result.add(java.lang.Long.valueOf(valAsString))
                    else if (typ == "java.lang.Float") result.add(java.lang.Float.valueOf(valAsString))
                    else if (typ == "java.lang.Double") result.add(java.lang.Double.valueOf(valAsString))
                    else if (typ == "java.lang.Boolean") result.add(java.lang.Boolean.valueOf(valAsString))
                    else if (typ == "java.lang.Character") result.add(new java.lang.Character(valAsString.charAt(0)))
                    else if (typ == "java.lang.String") result.add(valAsString)
                } else { // everything else is a string 
                    result.add(l)
                }
            }
        })
        assert(result.toString == s)
        result
    }
}    
