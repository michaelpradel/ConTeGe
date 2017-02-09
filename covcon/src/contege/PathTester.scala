package contege

import contege.seqgen.SeqTest
import contege.seqgen.TypeManager
import java.util.ArrayList
import java.io.File
import scala.collection.JavaConversions._
import contege.seqexec.reflective.SequenceManager
import contege.seqexec.reflective.SequenceExecutor
import scala.io.Source
import contege.seqgen.InstantiateCutTask
import contege.seqgen.Prefix
import contege.seqgen.StateChangerTask
import contege.seqexec.OutputVector
import contege.seqgen.CallTargetMethodTask
import contege.seqgen.TypedParameter
import contege.seqgen.TypedParameter
import contege.seqgen.TypedParameter
import scala.collection.mutable.Set
import contege.seqgen.GetParamTask

class PathTester(config: PathTesterConfig, envTypes: Seq[String], classLoader: ClassLoader, random: Random, stats: Stats) {
    
    private val typeMgr = new TypeManager(config.cut, envTypes, classLoader, random)
    private val seqExecutor = new SequenceExecutor(stats, config)
    private val seqMgr = new SequenceManager(seqExecutor, config, PathTester)
    
    if (!typeMgr.cutMethods.find(_.signature == config.targetCutMethod).isDefined) {
        println("Can't find target method - is it public and in one of the environment types?")
        PathTester.finalizeAndExit(false);
    }
    
    private var cutMethod = typeMgr.cutMethods.find(_.signature == config.targetCutMethod).get
    
    private var global = new GlobalState(config, typeMgr, seqMgr, stats, random, PathTester)
    
    def run = {
    	for (nbTests <- 1 to config.maxTests) {
    	    println("=========================== Test "+nbTests)
    	    createTest match {
    	        case Some(test) => {
    	        	println("\nCreated test:\n"+test)
		    	    if (checkPathMatch(test)) {
		    	        println("Path match!")
		    	        PathTester.finalizeAndExit(true)
		    	    } else {
		    	        println("No path match")
		    	    }
    	        }
    	        case None => {
    	            println("Failed to create test")
    	        }
    	    }
    	    println("------------------------------------------------\n")
    	}
    }
    
    private def createTest: Option[Prefix] = {
    	// TODO call other methods to set up state before calling the target method
        if (cutMethod.isStatic) {
            return callCutMethod(new Prefix(global))
        } else {
        	instantiateCut match {
        	    case Some(firstPart) => return callCutMethod(firstPart)
        	    case None => return None
        	}
        }
    }
    
    private def instantiateCut: Option[Prefix] = {
        new GetParamTask(new Prefix(global), config.cut, false, global).run match {
			case Some(prefix) => {
				prefix.fixCutVariable
				return Some(prefix)
			}
			case None => {
				return None
			}
		}
    }
    
    private def callCutMethod(prefix: Prefix): Option[Prefix] = {
        val task = new CallTargetMethodTask(prefix, global, cutMethod)
        task.run match {
        	case Some(extendedSequence) => {
        		return Some(extendedSequence)
        	}
        	case None => {
        	    return None
        	}
		}
    }
    
    private def checkPathMatch(test: Prefix): Boolean = {
        val (throwableOpt, outputVector) = seqExecutor.executeWithOutputVector(test)
        if (throwableOpt.isDefined) return false
        else {
            val result = outputVector.last
            if (result != OutputVector.NullValue) return true
            else return false
        }
    }
}

object PathTester extends Finalizer {

    val stats = new Stats 
    
    def main(args: Array[String]): Unit = {
    		assert(args.length == 6)
    		val targetMethodFile = args(0)
    		val targetMethodParametersFile = args(1)
    		val envTypesFile = args(2)
    		val seed = args(3).toInt
    		val maxTests = args(4).toInt
    		val resultFile = args(5)
    		
    		val (cut, cutMethod) = parseTargetMethodFile(targetMethodFile);
    		
    		val targetMethodParameters = parseTargetMethodParameters(targetMethodParametersFile)
    		
    		val envTypes = new ArrayList[String]()
	    	Util.addEnvTypes(envTypesFile, envTypes, getClass.getClassLoader)
	    	
	    	val classLoader = getClass.getClassLoader // TODO use unprivileged classloader
	    	
    		val workingDir = new File("/tmp/pathTesterWorkingDir")
    		val random = new Random(seed)
    		val config = new PathTesterConfig(cut, cutMethod, targetMethodParameters, seed, maxTests, workingDir)
    		
    		new PathTester(config, envTypes.toList, classLoader, random, stats).run
    }
    
    def parseTargetMethodFile(fileName: String) = {
        val line = Source.fromFile(fileName).getLines.next()
        val splitted = line.split(": ")
        assert(splitted.length == 2)
        val cut = splitted(0)
        val cutMethod = splitted(0)+"."+splitted(1)
        Pair(cut, cutMethod)
    }
    
    def parseTargetMethodParameters(fileName: String) = {
    	val result = Set[List[Option[TypedParameter]]]()
        for (line <- Source.fromFile(fileName).getLines) {
        	var params = new ArrayList[Option[TypedParameter]]
            line.split(",").foreach(paramDescr => {
                if (paramDescr == "*") {
                	params.add(None)
                } else {
	                val typeAndVal = paramDescr.split(" ")
	                params.add(Some(new TypedParameter(typeAndVal(0), typeAndVal(1))))
            	}
            })
        	result.add(params.toList)
        }
        result
    }
    
    def finalizeAndExit(bugFound: Boolean) = {
        println("Found bug: "+bugFound)
        stats.print
        System.exit(0)
    }
}