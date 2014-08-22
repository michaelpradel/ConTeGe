package contege.tools

import scala.collection.JavaConversions._
import java.io.BufferedReader
import java.io.FileReader
import java.util.ArrayList
import java.lang.reflect.Modifier

object PublicAndConcreteClasses {
    def main(args : Array[String]) : Unit = {
    	
    	val allClassesFile = args(0)
    	val r = new BufferedReader(new FileReader(allClassesFile))
    	val allClasses = new ArrayList[String]()
    	var line = r.readLine
    	while (line != null) {
    		allClasses.add(line)
    		line = r.readLine
    	}
    	r.close
    	
    	allClasses.foreach(className => {
    		val cls = Class.forName(className)
    		if (Modifier.isPublic(cls.getModifiers) &&
    			!Modifier.isAbstract(cls.getModifiers) &&
    			cls.getConstructors.exists(constr => Modifier.isPublic(constr.getModifiers))) println(className)    		
    	})	
    	
    }
}
