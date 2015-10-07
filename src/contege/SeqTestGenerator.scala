package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.util.Date
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.io.BufferedReader
import java.io.FileReader
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.PrintStream
import gov.nasa.jpf.Error
import contege.seqexec.jpf.RobustConcJPFExecutor
import contege.seqexec.OutputVector
import contege.seqexec.DeadlockMonitor
import contege.seqgen.AppendPreparedCUTCallTask
import contege.seqgen.PrepareCUTCallTask
import contege.seqgen.Suffix
import contege.seqgen.Prefix
import contege.seqgen.InstantiateCutTask
import contege.seqgen.TypeManager
import contege.seqgen.StateChangerTask
import contege.seqgen.Variable
import contege.seqexec.TestPrettyPrinter
import java.io.File
import contege.seqexec.reflective.SequenceManager
import contege.seqexec.reflective.SequenceExecutor
import contege.seqexec.JavaSkeleton
import contege.seqgen.CallTargetMethodTask

/**
 * Generates sequential unit tests. Arguments:
 *  - Fully-qualified name of class under test.
 *  - Path to file with environment types.
 *  - Random seed.
 *  - Maximum number of tests (currently, only 1 is allowed).
 *  - Maximum number of calls in a test. "allMUTS" means to try to call all methods of the class under test.
 *  - Path for the result file.
 * The generated test is written into SeqTeGeTest.java.
 * 
 * 
 * @author Ankit Choudhary, Michael Pradel
 */
class SeqTestGenerator(config: SeqTestGenConfig, stats: Stats, putClassLoader: ClassLoader,
                     envTypes: ArrayList[String], random: Random, finalizer: Finalizer, maxStateChangersInPrefix: Int) {
	
	private val typeManager = new TypeManager(config.cut, envTypes, putClassLoader, random)

	private val seqMgr = new SequenceManager(new SequenceExecutor(stats, config), config, finalizer)

	private val global = new GlobalState(config, typeManager, seqMgr, stats, random, finalizer) 
	
	private val javaSkeletonObj = new JavaSkeleton("","SeqTeGeTest","seqTeGeTest")
	
	def run() = {
	    println("SeqTestGenerator starting at "+new Date)
		
		val cutMethods = typeManager.cutMethods
		assert(!cutMethods.isEmpty, "No methods in the class that we could test: "+config.cut)
		println("maxPairs="+config.maxTests)
		for (i <- 1 to config.maxTests) {
			println(i)
		    val test = testSequentially(cutMethods, config)
		    javaSkeletonObj.javaCodeFor(test)
		}
	    javaSkeletonObj.writeJavaTest
	}
	
	private def testSequentially(cutMethods: Seq[MethodAtom], config: SeqTestGenConfig) = {
	    val cutInstantiated = instantiateCUT()
	    if (maxStateChangersInPrefix < Integer.MAX_VALUE)
	    	appendStateChangers(cutInstantiated)
	    else {
	        // "allMUTs" case -- try to call all methods of the CUT
	        println("Running with allMUTs option. "+cutMethods.length+" MUTs to call.")
	        println(cutMethods.toList.mkString("\n"))
	        var currentSequence = cutInstantiated
	        var methodsToCall = cutMethods.toList
	        var calledMUTs = 0
	        var blockedMethods = new ArrayList[MethodAtom]() // methods that we apparently cannot call
	        while (!methodsToCall.isEmpty && blockedMethods.length < methodsToCall.length) {
	            val targetMethod = random.chooseOne(methodsToCall.diff(blockedMethods))
	            val callTargetMethodTask = new CallTargetMethodTask(currentSequence, global, targetMethod)
	            callTargetMethodTask.run match {
	                case Some(extendedSequence) => {
	                    methodsToCall = methodsToCall.filter(_ != targetMethod)
	                    blockedMethods = new ArrayList[MethodAtom]()
	                    currentSequence = extendedSequence
	                    calledMUTs += 1
	                }
	                case None => {
	                    blockedMethods.add(targetMethod)
	                }
	            }
	        }
	        
	        if (!methodsToCall.isEmpty)
	            println("Warning: Could only call "+calledMUTs+" of "+cutMethods.length+" MUTs.")
	        currentSequence
	    }
	}
	
	private def instantiateCUT(): Prefix = {
		new InstantiateCutTask(global).run match {
			case Some(prefix) => {
				prefix.fixCutVariable
				return prefix	
			}
			case None => {
				println("Giving up to create CUT: "+config.cut)
				finalizer.finalizeAndExit(false)
				throw new IllegalStateException("should never be reached")
			}
		}
	}
	
	private def appendStateChangers(prefix: Prefix) = {
		var currentSequence = prefix
		for (_ <- 1 to maxStateChangersInPrefix) {
			val stateChangerTask = new StateChangerTask(currentSequence, global)
			stateChangerTask.run match {
				case Some(extendedSequence) => {
					currentSequence = extendedSequence
				}
				case None => // ignore
			}
		}
		currentSequence
	}
	
	private def prepareForCalling(prefix: Prefix, method: MethodAtom): Option[(Prefix, ArrayList[Variable])] = {
		val task = new PrepareCUTCallTask(prefix, method, global)
		task.run match {
			case Some(extendedPrefix) => {
				assert(task.args.isDefined)
				return Some(extendedPrefix, task.args.get)
			}
			case None => {
				println("Giving up to prepare for calling: "+method)
				return None
			}
		}
	}
	
	private def appendSuffixCall(suffix: Suffix, method: MethodAtom, args: ArrayList[Variable]): Suffix = {
		new AppendPreparedCUTCallTask(suffix, method, args, global).run match {
			case Some(extendedSuffix) => return extendedSuffix
			case None => {
				println("Couldn't append the CUT call that we prepared for. This should never happen!\n"+suffix+"\n--> Method: "+method)
				finalizer.finalizeAndExit(false)
				throw new IllegalStateException("should never be reached")
			}
		}
	}
	
	
}

object SeqTestGenerator extends Finalizer {
	
	private var stats: Stats = _
	private var config: SeqTestGenConfig = _
	
    def main(args : Array[String]) : Unit = {
	    // read config from command line arguments
	    assert(args.size == 6, args.size)
	    
	    val cut = args(0)
	    
	    val seed = args(2).toInt
	    val random = new Random(seed)
	    
	    val envTypesFile = args(1)
	    val envTypes = new ArrayList[String]()
	    envTypes.add("java.lang.Object")
	    Util.addEnvTypes(envTypesFile, envTypes, getClass.getClassLoader)
	    
	    val resultFile = args(5)
	    
	    val maxTests = args(3).toInt
	    
	    val maxCallInTest = if (args(4) == "allMUTs") Integer.MAX_VALUE else args(4).toInt 
	    
	    val compareOutputVectors = true
	    val stopOnOutputDiff = false
	    
	    config = new SeqTestGenConfig(cut, seed, 0, maxTests, None, new File("/tmp/"))
	    
	    config.addCheckerListener(new ResultFileCheckerListener(resultFile))
	    
		stats = new Stats
    	val tester = new SeqTestGenerator(config, stats, getClass.getClassLoader, envTypes, random, this, maxCallInTest)
    	tester.run()
    	
    	finalizeAndExit(false)
    }
    
    override def finalizeAndExit(bugFound: Boolean) = {
        stats.print        
        stats.timer.print
	    println("Done with SeqTestGenerator at "+new Date)
	    
        config.checkerListeners.foreach(_.notifyDoneNoBug) // TODO notifyNoBug may not be appropriate, but it does what it should for ResultFileCheckerListener
        
		Console.out.flush
		Console.err.flush
		System.exit(0)
	}

}

