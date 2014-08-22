package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList

class ConstructorMapping {

    private val map = Map[String,(String,List[Int])]()
    
    /**
     * destParamPositions starts to count at 1; e.g. (2,1) means that the 2nd parameter of the src constructor will be the 1st parameter of the dest. constructor
     */
    def add(srcParams: String, destParams: String, destParamPositions: List[Int]) = {
        map.put(srcParams, (destParams, destParamPositions))
    }
    
    def destParams(srcParams: String) = map(srcParams)._1
    
    def destParamPositions(srcParams: String) = map(srcParams)._2
 
    def mappable(srcParams: String) = map.contains(srcParams)
    
    def size = map.size
    
    override def toString = {
        val sb = new StringBuilder
        for ((superC, (subC, pos)) <- map) {
            sb.append(superC+"@"+subC+"@"+pos.mkString(",")+"\n")
        }
        sb.toString
    }
    
}

object ConstructorMapping {
    val default = new ConstructorMapping
    default.add("()", "()", List[Int]())
}