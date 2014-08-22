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

class SubclassTester(config: SubclassTesterConfig, stats: Stats, putClassLoader: ClassLoader,
                     envTypes: ArrayList[String], random: Random, finalizer: Finalizer) {
	
	private val maxStateChangersInPrefix = 5
	
	private val typeManager = new TypeManager(config.cut, envTypes, putClassLoader, random)
	typeManager.filterCUTMethods(config.oracleClass)

	private val seqMgr = new SequenceManager(new SequenceExecutor(stats, config), config, finalizer)

	private val global = new GlobalState(config, typeManager, seqMgr, stats, random, finalizer) 
	
    private val jpfExecutor = new RobustConcJPFExecutor(global, ".", config.compareOutputVectors)
	
	private val fullOutput = if (config.compareOutputVectors) TestPrettyPrinter.FullOutputVectors else TestPrettyPrinter.NoOutputVectors
	private val suffixOutput = if (config.compareOutputVectors) TestPrettyPrinter.SuffixOutputVectors else TestPrettyPrinter.NoOutputVectors
	
	def run() = {
	    println("SubclassTester starting at "+new Date)
	    
		val dlMonitor = new DeadlockMonitor(config, global)
		dlMonitor.setDaemon(true)
		dlMonitor.start
		
		val cutMethods = typeManager.cutMethods
		assert(!cutMethods.isEmpty, "No methods in the class that we could test: "+config.cut)
		println("maxPairs="+config.maxTests)
		for (i <- 1 to config.maxTests) {
		    if (config.concurrentMode) testConcurrently(cutMethods, config, i)
		    else testSequentially(cutMethods, config, i)
		}
	}
	
	private def testConcurrently(cutMethods: Seq[MethodAtom], config: SubclassTesterConfig, testCtr: Int) = {
    	// choose two methods to run against each other 
		val method1 = random.chooseOne(cutMethods)
		val method2 = random.chooseOne(cutMethods)
		
		println("\n-------------------Next pair starting------------------\nCUT methods: "+method1+" vs "+method2+"   -- pair "+testCtr)
			
		// generate prefix
		val cutInstantiated = instantiateCUT()
		val stateChangersCalled = appendStateChangers(cutInstantiated)
		prepareForCalling(stateChangersCalled, method1) match {
		    case Some((preparedForCall1, argsForCall1)) => {
		        prepareForCalling(preparedForCall1, method2) match {
		            case Some((prefix, argsForCall2)) => {
		            	// generate suffixes
						val emptySuffix1 = new Suffix(prefix, global)
						val suffix1 = appendSuffixCall(emptySuffix1, method1, argsForCall1)
						val emptySuffix2 = new Suffix(prefix, global)
						val suffix2 = appendSuffixCall(emptySuffix2, method2, argsForCall2)
					
					    // analyze concurrent usage of subclass
					    val (sutErrorsOpt, sutOutputVectorsOpt) = jpfExecutor.executeConcurrently(prefix, suffix1, suffix2, suffixOutput)
					    sutErrorsOpt match {
					        case Some(sutErrors) => {
					            stats.succJPFRuns.incr
					        	if (!sutErrors.isEmpty) {
					        	    println("Executing with subclass leads to failure .. testing with superclass ..")
							        // errors with subclass -- check if they also happen with superclass
					        	    val oraclePrefix = prefix.copyWithOracleClassReceiver(config.oracleClass)
					        	    val oracleSuffix1 = suffix1.copyWithOracleClassReceiver(config.oracleClass, oraclePrefix)
					        	    val oracleSuffix2 = suffix2.copyWithOracleClassReceiver(config.oracleClass, oraclePrefix)
					        	    
					        	    // analyze concurrent usage of superclass
							        val (superClassErrorsOpt, superClassOutputVectorsOpt) = jpfExecutor.executeConcurrently(oraclePrefix, oracleSuffix1, oracleSuffix2, suffixOutput)
							        superClassErrorsOpt match {
							            case Some(superClassErrors) => {
							                stats.succJPFRuns.incr
							            	if (superClassErrors.isEmpty) { // report warning only if superclass has no errors (misses case where superclass has some errors but not all, but it's hard to compare exceptions precisely)
							            	    // check that the prefix alone runs successfully in JPF (should be true, but we only tested the prefix through real (i.e. non-JPF) execution)
							            	    if (checkPrefixInJPF(prefix)) {
							            	        config.checkerListeners.foreach(_.appendResultMsg("@@@ Found thread safety violation @@@"))
													config.checkerListeners.foreach(_.appendResultMsg("--- Prefix:"))
													config.checkerListeners.foreach(_.appendResultMsg(prefix.toString))
													config.checkerListeners.foreach(_.appendResultMsg("--- Suffix 1:"))
													config.checkerListeners.foreach(_.appendResultMsg(suffix1.toString))
													config.checkerListeners.foreach(_.appendResultMsg("--- Suffix 2:"))
													config.checkerListeners.foreach(_.appendResultMsg(suffix2.toString))
													config.checkerListeners.foreach(_.appendResultMsg("--- Result:"))
													
													sutErrors.foreach(e => config.checkerListeners.foreach(_.appendResultMsg(e)))
													finalizer.finalizeAndExit(true)
							            	    } else println("Ignoring test because its prefix leads to error in JPF but not with normal execution (JPF limitation?)")
									        } else {
									            println("Execution with superclass also leads to failure - won't report anything.")
									        }					                
							            }
							            case None => {
							                // no result from JPF (e.g. timeout) -- ignore this test
							                stats.inconclusiveJPFRuns.incr
							            	println("Ignoring test because concurrent JPF execution for superclass has no result")                
							            }
							        }
							    } else {
							        // JPF run for subclass was successful: compare output vectors to those of superclass (if enabled)
							        if (config.compareOutputVectors) {
							            val oraclePrefix = prefix.copyWithOracleClassReceiver(config.oracleClass)
						        	    val oracleSuffix1 = suffix1.copyWithOracleClassReceiver(config.oracleClass, oraclePrefix)
						        	    val oracleSuffix2 = suffix2.copyWithOracleClassReceiver(config.oracleClass, oraclePrefix)

						        	    // analyze concurrent usage of superclass
								        val (superClassErrorsOpt, superClassOutputVectorsOpt) = jpfExecutor.executeConcurrently(oraclePrefix, oracleSuffix1, oracleSuffix2, suffixOutput)
								        superClassErrorsOpt match {
								            case Some(superClassErrors) => {
								                stats.succJPFRuns.incr
								                if (superClassErrors.isEmpty) {
								                    // compare output vectors
								                    val subOVs = sutOutputVectorsOpt.get
								                    val superOVs = superClassOutputVectorsOpt.get
								                    val subOnlyOVs = subOVs -- superOVs
								                    if (!subOnlyOVs.isEmpty && !superOVs.isEmpty) {
							                            // some Sub output can't occur with Super
							                            // --> check if it also appears when linearizing the test (if yes, no conc. pb)
							                            assert(suffix1.length == 1 && suffix2.length == 1)
							                            val nonVoidSuffixMethods = suffix1.nbNonVoidCalls + suffix2.nbNonVoidCalls
							                            val interleavings = new SequentialInterleavings(prefix, suffix1, suffix2)
							                            var interleavingOpt = interleavings.nextInterleaving
							                            val linearizedOVs = Set[OutputVector]()
							                            while (interleavingOpt.isDefined) {
							                                val interleaving = interleavingOpt.get
							                                // use JPF to ensure we get the same output as in the concurrent exploration with JPF
							                                val (errorsOpt, outputsOpt) = jpfExecutor.executeConcurrently(interleaving, new Suffix(interleaving, global), new Suffix(interleaving, global), fullOutput) // ideally, should have a method to execute sequentially in JPF
							                                assert(errorsOpt.isDefined)
							                                assert(errorsOpt.get.isEmpty)
							                                assert(outputsOpt.get.size == 1)
							                                val interleavingOutput = outputsOpt.get.find(_=>true).get.lastNOutputsOnly(nonVoidSuffixMethods)
							                                linearizedOVs.add(interleavingOutput)
							                                
							                                interleavingOpt = interleavings.nextInterleaving
							                            }
							                            
							                            val subOnlyConcOnlyOVs = subOnlyOVs.filter(sub => !linearizedOVs.exists(lin => sub.sameSetAs(lin)))
						                                if (!subOnlyConcOnlyOVs.isEmpty) {
						                                    // there's an output that happens only for Sub and only concurrently -- report it!
						                                	if (config.stopOnOutputDiff) {
						                                		config.checkerListeners.foreach(_.appendResultMsg("@@@ Found output(s) produced only by subclass @@@"))
																config.checkerListeners.foreach(_.appendResultMsg("--- Prefix:"))
																config.checkerListeners.foreach(_.appendResultMsg(prefix.toString))
																config.checkerListeners.foreach(_.appendResultMsg("--- Suffix 1:"))
																config.checkerListeners.foreach(_.appendResultMsg(suffix1.toString))
																config.checkerListeners.foreach(_.appendResultMsg("--- Suffix 2:"))
																config.checkerListeners.foreach(_.appendResultMsg(suffix2.toString))
																config.checkerListeners.foreach(_.appendResultMsg("--- Subclass-only outputs:"))
																subOnlyConcOnlyOVs.foreach(ov => config.checkerListeners.foreach(_.appendResultMsg(ov+"\n(end of output vector)\n")))
																config.checkerListeners.foreach(_.appendResultMsg("--- Superclass outputs:"))
																superOVs.foreach(ov => config.checkerListeners.foreach(_.appendResultMsg(ov+"\n(end of output vector)\n")))
																finalizer.finalizeAndExit(true)	
						                                	} else {
						                                		// write to stdout; result outputs should only be written when we want to terminate
						                                		println("@@@ Found output(s) produced only by subclass @@@")
																println("--- Prefix:")
																println(prefix.toString)
																println("--- Suffix 1:")
																println(suffix1.toString)
																println("--- Suffix 2:")
																println(suffix2.toString)
																println("--- Subclass-only outputs:")
																subOnlyConcOnlyOVs.foreach(ov => println(ov+"\n(end of output vector)\n"))
																println("--- Superclass outputs:")
																superOVs.foreach(ov => println(ov+"\n(end of output vector)\n"))
						                                	}
						                                }						                                
								                    }
								                } else {
								                    // test fails for superclass -- can't conclude anything -- ignore this test
								                    println("Ignoring test because concurrent JPF execution for superclass fails while it succeeds for subclass")
								                }
								            }
								            case None => { // no result from JPF for superclass -- ignore this test
								            	stats.inconclusiveJPFRuns.incr
								            	println("Ignoring test because concurrent JPF execution for superclass has no result (2)")	
								            }
							            }
							        }
							    }		            
					        }
					        case None => {
					            // no result from JPF (e.g. because of timeout) -- ignore this test
					            stats.inconclusiveJPFRuns.incr
					            println("Ignoring test because concurrent JPF execution for subclass has no result")
					        }
					    }
						if (testCtr % 10 == 0) println("Tested "+testCtr+" pairs of methods")			                
		            }
		            case None => // ignore this pair of methods
		        }
		    }
		    case None => // ignore this pair of methods
		}
	}
	
	/**
	 * Returns true if prefix runs without errors through JPF.
	 */
	private def checkPrefixInJPF(prefix: Prefix): Boolean = {
	    val (prefixErrorsOpt, _) = jpfExecutor.executeConcurrently(prefix, new Suffix(prefix, global), new Suffix(prefix, global), TestPrettyPrinter.NoOutputVectors)
	    prefixErrorsOpt match {
	        case Some(prefixErrors) => {
	            if (prefixErrors.isEmpty) return true
	            else return false
	        }
	        case None => false
	    }
	}
	
	private def testSequentially(cutMethods: Seq[MethodAtom], config: SubclassTesterConfig, testCtr: Int) = {
	    // superclass oracle is called for each failing sequence by SequenceManager
	    val cutInstantiated = instantiateCUT()
		appendStateChangers(cutInstantiated)
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

object SubclassTester extends Finalizer {
	
	private var stats: Stats = _
	private var config: SubclassTesterConfig = _
	
    def main(args : Array[String]) : Unit = {
	    // read config from command line arguments
	    assert(args.size == 6, args.size)
	    
	    val sutFile = args(0)
	    
	    val seed = args(2).toInt
	    val random = new Random(seed)
	    
	    val envTypesFile = args(1)
	    val envTypes = new ArrayList[String]()
	    envTypes.add("java.lang.Object")
	    Util.addEnvTypes(envTypesFile, envTypes, getClass.getClassLoader)
	    
	    val simpleSutParser = new SimpleSUTParser(sutFile)
	    
	    val typeManager = new TypeManager(simpleSutParser.subCls, envTypes, getClass.getClassLoader, random)

  	    val sutParser = new SUTParser(sutFile, typeManager)
	    
	    val resultFile = args(4)
	    
	    val maxTests = args(3).toInt
	    
	    val concMode = if (args(5) == "concurrent") true else if (args(5) == "sequential") false else throw new IllegalArgumentException(args(5))
	    if (concMode) println("Testing in concurrent mode")
	    else println("Testing in sequential mode")
	    
	    val compareOutputVectors = true
	    val stopOnOutputDiff = false

	    val workingDir = new File(sutFile).getAbsoluteFile.getParentFile
	    assert(workingDir.isDirectory)
	    
	    config = new SubclassTesterConfig(sutParser.subCls, sutParser.superCls, sutParser.constrMapping, seed, 100, maxTests, concMode, None, workingDir, compareOutputVectors, stopOnOutputDiff)
	    
	    config.addCheckerListener(new ResultFileCheckerListener(resultFile))
	    
		stats = new Stats
    	val tester = new SubclassTester(config, stats, getClass.getClassLoader, envTypes, random, this)
    	tester.run()
    	
    	finalizeAndExit(false)
    }
    
    override def finalizeAndExit(bugFound: Boolean) = {
        stats.print        
	    println("Done with SubclassTester at "+new Date)
	    
        config.checkerListeners.foreach(_.notifyDoneNoBug) // TODO notifyNoBug may not be appropriate, but it does what it should for ResultFileCheckerListener
        
		Console.out.flush
		Console.err.flush
		System.exit(0)
	}

}

