package contege.tools

import scala.collection.JavaConversions._
import java.io.BufferedReader
import java.io.FileReader
import java.util.ArrayList
import java.lang.reflect.Modifier
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.PrintWriter

object PublicAndConcreteClasses {
    def main(args : Array[String]) : Unit = {
    	
        assert(args.length == 2)
        
    	val allClassesFile = args(0)
    	val r = new BufferedReader(new FileReader(allClassesFile))
    	val allClasses = new ArrayList[String]()
    	var line = r.readLine
    	while (line != null) {
    		allClasses.add(line)
    		line = r.readLine
    	}
    	r.close
    	
    	val out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(args(1))))
    	allClasses.foreach(className => {
    	    try {
	    		val cls = Class.forName(className, true, getClass.getClassLoader)
	    		if (Modifier.isPublic(cls.getModifiers) &&
	    			!Modifier.isAbstract(cls.getModifiers) /* &&
	    			cls.getConstructors.exists(constr => Modifier.isPublic(constr.getModifiers))*/) {
	    		    out.println(className)
	    		} 
    	    } catch {
    	        case _: Throwable => // ignore unloadable classes
    	    }
    	})
    	out.close
    	
    }
}
