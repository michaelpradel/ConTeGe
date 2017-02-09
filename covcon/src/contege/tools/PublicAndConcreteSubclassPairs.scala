package contege.tools

import java.io.BufferedReader
import java.io.FileReader
import java.io.PrintStream
import java.lang.reflect.Modifier
import java.util.ArrayList
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import scala.collection.JavaConversions._

object PublicAndConcreteSubclassPairs extends App {
    
    val targetDir = "/tmp/suts"
    
	val allClassesFile = args(0)
	val r = new BufferedReader(new FileReader(allClassesFile))
	val allClasses = new ArrayList[String]()
	var line = r.readLine
	while (line != null) {
		allClasses.add(line)
		line = r.readLine
	}
	r.close

	val sub2Supers = Map[Class[_], Set[Class[_]]]()
	
	allClasses.foreach(className => {
	    try {
			val cls = Class.forName(className)
			if (className.startsWith("org.apache.commons.collections")) {
			    if (Modifier.isPublic(cls.getModifiers) &&
				    !Modifier.isAbstract(cls.getModifiers) &&
				    cls.getConstructors.exists(constr => Modifier.isPublic(constr.getModifiers))) {
			    	val allSuper = allSuperClasses(cls)
			    	allSuper.foreach(superCls => {
			    	    if (Modifier.isPublic(superCls.getModifiers) &&
			    	    	!Modifier.isAbstract(superCls.getModifiers) &&
			    	    	superCls.getConstructors.exists(constr => Modifier.isPublic(constr.getModifiers))) {
			    	        if (superCls.getName != "java.lang.Object" && !cls.getName.endsWith("Exception") && !superCls.getName.endsWith("Exception")) {
			    	        	println(cls.getName+" -- "+superCls.getName)
			    	        	sub2Supers.getOrElseUpdate(cls, Set[Class[_]]()).add(superCls)
			    	        }
			    	    }
			    	})
			    }
			}
	    } catch {
	        case t: Throwable => println(">> Ignoring class "+className+" -- "+t)
	    }
	})
	
    // put .sut files in target dir
	for ((sub, supers) <- sub2Supers) {
	    var superCtr = 1
	    supers.foreach(sup => {
	        val s = new PrintStream(targetDir+"/"+sub.getSimpleName+superCtr+".sut")
	        s.println("SUPER="+sup.getName)
	        s.println("SUB="+sub.getName)
	        s.println("MATCH_CONSTRUCTORS_BY_PARAM_TYPE")
	        s.close
	        
	        superCtr += 1
	    })
	}
	
	private def allSuperClasses(cls: Class[_]): Set[Class[_]] = {
	    val result = Set[Class[_]]()
	    if (cls.getName != "java.lang.Object") {
	        result.add(cls.getSuperclass)
	        result.addAll(allSuperClasses(cls.getSuperclass))
	    }
	    result
	}
	
}