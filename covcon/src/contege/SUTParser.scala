package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.io.BufferedReader
import java.io.FileReader
import contege.seqgen.TypeManager

class SUTParser(fileName: String, typeManager: TypeManager) {

    private var superClsOpt: Option[String] = None
    private var subClsOpt: Option[String] = None
    val constrMapping = new ConstructorMapping
    
	private val r = new BufferedReader(new FileReader(fileName))
	private var line = r.readLine
	private var ctr = 1
	while (line != null) {
	    if (ctr == 1) {
	        assert(line.contains("SUPER="))
	        superClsOpt = Some(line.drop("SUPER=".length))
	    } else if (ctr == 2) {
	        assert(line.contains("SUB="))
	        subClsOpt = Some(line.drop("SUB=".length))
	    } else {
	        // constructor mapping part
	        if (line == "MATCH_CONSTRUCTORS_BY_PARAM_TYPE") {
	            // initialize constructor mapping by mapping constructors by their parameter type(s)
	            assert(constrMapping.size == 0)
	            addTypeBasedConstrMappings
	        } else {
	            // add the given mapping to the current constructor map
	        	val splitted = line.split("@")
		        assert(splitted.size == 2 || splitted.size == 3)
		        val destParamPositions = if (splitted.size == 2) {
		            List[Int]()
		        } else {
		            splitted(2).split(",").map(_.toInt).toList
		        }
		        constrMapping.add(splitted(0), splitted(1), destParamPositions)
	        }
	    }
	    
	    line = r.readLine
	    ctr += 1
	}
	r.close
    		
	def superCls = superClsOpt.get
	
	def subCls = subClsOpt.get
	
	private def addTypeBasedConstrMappings = {
	    val superConstructors = typeManager.constructors(superCls)
	    val subConstructors = typeManager.constructors(subCls)
	    superConstructors.foreach(superConstr => {
	        subConstructors.find(subConstr => superConstr.paramTypes == subConstr.paramTypes) match {
	            case Some(subConstr) => {
		            val superParams = superConstr.paramTypes.mkString("(",",",")")
		            val subParams = subConstr.paramTypes.mkString("(",",",")")
		            val destParamPositions = Range(1, superConstr.paramTypes.size+1).toList
		            constrMapping.add(superParams, subParams, destParamPositions)
	            }
	            case None => // ignore this super constructors, cannot map it
	        }
	    })
	}	
}

class SimpleSUTParser(fileName: String) {
    private var superClsOpt: Option[String] = None
    private var subClsOpt: Option[String] = None
    
    private val r = new BufferedReader(new FileReader(fileName))

    private var firstLine = r.readLine
	assert(firstLine.contains("SUPER="))
	superClsOpt = Some(firstLine.drop("SUPER=".length))
    
	private var secondLine = r.readLine
	assert(secondLine.contains("SUB="))
	subClsOpt = Some(secondLine.drop("SUB=".length))
	
    def subCls = subClsOpt.get
        
    def superCls = superClsOpt.get
}