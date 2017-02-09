package jdkAnalysis

import scala.io.Source

object ClassLoaderTest {

    def main(args: Array[String]): Unit = {
        
        assert(args.length == 1)
        
        val classesFile = args(0)
        
        val classes = Source.fromFile(classesFile).getLines
        classes.foreach(clsName => {
//            println(clsName)
            try {
                val cls = Class.forName(clsName, false, null)
                if (cls.getClassLoader != null)
                    println("Non-null classloader: "+clsName)
            } catch {
                case _ => println("Some problem with "+clsName+" -- ignore")
            }
            	
        })
        
//        println(Class.forName("jdkAnalysis.ABC").getClassLoader)
//        println(this.getClass.getClassLoader)
//        println(Class.forName("java.util.ArrayList", false, this.getClass.getClassLoader).getClassLoader)
        
        
    }
    
    
}

class ABC {}
