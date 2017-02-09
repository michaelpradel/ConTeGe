package jdkTest

import scala.io.Source

object ClassLoadingTest extends App {

//    val cls = Class.forName("sun.misc.Unsafe")
//    println(cls)		
    
   Source.fromFile("/home/m/research/experiments/java_permissions/all_jdk_classes_1.7.0_u3.txt").getLines.foreach(line => {
//      println(line)
      try {
          Class.forName(line)
      } catch {
          case e => println(line)
      }
   })
    
}
