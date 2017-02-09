package contege

import contege.seqgen.Call
import contege.seqgen.Constant
import contege.seqgen.NullConstant
import contege.seqgen.Variable
import org.apache.bcel.generic.Type
import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import scala.collection.mutable.Set
import contege.seqexec.OutputVector

object SequencePrinter {

    class OutputConfig
    object NoOutput extends OutputConfig
    object FillOutputVector extends OutputConfig
    object FillOutputVectorWithIgnore extends OutputConfig
    
    def callToString(call: Call, id: Variable => String, outputConfig: OutputConfig) = {
		val sb = new StringBuilder
		if (call.retVal.isDefined) {
		    var newVarType = javaType(if (call.downcastType.isDefined) call.downcastType.get else call.atom.returnType.get)
			sb.append("final "+newVarType+" "+id(call.retVal.get)+" = "+(if (call.downcastType.isDefined) "("+call.downcastType.get+") " else ""))    
		}
		sb.append(if (call.atom.isConstructor) "new "+javaType(call.atom.declaringType)
				  else if (call.atom.isStatic) javaType(call.atom.declaringType)+"."+call.atom.methodName
				  else id(call.receiver.get)+"."+call.atom.methodName)
	    if (!call.atom.isField) {
		    sb.append("(")
		    // cast each argument to ensure that we call the atom we want to call (important for overloaded methods)
		    val castedArgString = new ArrayList[String]()
		    for (pos <- 0 to call.atom.paramTypes.size - 1) {
		        val arg = call.args(pos)
		        val argString = if (arg.isInstanceOf[Constant]) constantToString(arg.asInstanceOf[Constant])
								else if (arg == NullConstant) "null"
					            else id(arg)
			    val typeString = castType(javaType(call.atom.paramTypes(pos)))
			    castedArgString.add("("+typeString+") "+argString)
		    } 
		    sb.append(castedArgString.mkString(", "))
			sb.append(");")    
	    } else {
	        sb.append(";")
	    }
		
		// add returned value to output vector
		outputConfig match {
		    case NoOutput => // do nothing
		    case FillOutputVector => sb.append("\noutputVector.add("+id(call.retVal.get)+");\n")
		    case FillOutputVectorWithIgnore => sb.append("\noutputVector.add(\""+OutputVector.IgnoreString+"\");\n")
		}
		
		sb.toString
	}
	
	private def constantToString(c: Constant): String = {
		c.value match {
			case i: java.lang.Integer => i.toString
			case b: java.lang.Byte => "(byte)"+b.toString
			case s: java.lang.Short => "(short)"+s.toString
			case l: java.lang.Long => l.toString+"L"
			case c: java.lang.Character => "'"+c.toString+"'"
			case b: java.lang.Boolean => b.toString
			case f: java.lang.Float => f.toString+"f"
			case d: java.lang.Double => d.toString+"d"
			case s: java.lang.String => "\""+s+"\""
			case _ => throw new RuntimeException("unknown constant "+c+" of type "+c.getClass.getName)
		}
	}
	
	private def castType(t: String) = {
	    val boxed2Prim = Map("java.lang.Integer" -> "int",
	                         "java.lang.Byte" -> "byte",
	                         "java.lang.Short" -> "short",
	                         "java.lang.Long" -> "long",
	                         "java.lang.Character" -> "char",
	                         "java.lang.Boolean" -> "boolean",
	                         "java.lang.Float" -> "float",
	                         "java.lang.Double" -> "double")
	    boxed2Prim.get(t) match {
	        case Some(prim) => prim
	        case None => t
	    }
	}
	
	private def javaType(binType: String) = {
	    if (binType.contains('[')) { // array type
	    	Type.getType(binType).toString	        
	    } else if (binType.contains('$')) {
	        binType.replaceAll("\\$", ".")
	    } else binType
	}
    
}