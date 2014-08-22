package contege

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URL
import java.net.URLClassLoader
import java.util.ArrayList
import java.util.Date
import scala.annotation.elidable
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListMap
import scala.collection.mutable.Map
import scala.util.MurmurHash
import annotation.elidable.ASSERTION
import contege.seqexec.reflective.SequenceExecutor
import contege.seqexec.reflective.SequenceManager
import contege.seqexec.reflective.TSOracleNormalExec
import contege.seqexec.DeadlockMonitor
import contege.seqgen.InstantiateCutTask
import contege.seqgen.Prefix
import contege.seqgen.StateChangerTask
import contege.seqgen.Suffix
import contege.seqgen.SuffixGen
import contege.seqgen.TypeManager
import contege.seqgen.Variable
import javamodel.util.TypeResolver
import java.util.concurrent.TimeUnit

class ClassTester(config: Config, stats: Stats, classLoaderV0: CustomClassLoader, classLoaderV1: CustomClassLoader, classLoaderV2: CustomClassLoader,
	putJarPath: String, random: Random, finalizer: Finalizer,
	val focusMethods: Option[scala.collection.immutable.Map[String, Int]], val typeProvider: TypeManager) {

	private val prefixes = new ArrayList[Prefix] // kept separately to ensure deterministic random selection of one
	private val prefix2SuffixGen = Map[Prefix, SuffixGen]()
	private val prefix2Suffixes = Map[Prefix, ArrayList[Suffix]]()
	private val prefix2PrefixV1 = Map[Prefix, (Prefix, ListMap[Option[Variable], Option[Variable]])]()
	private val prefix2PrefixV2 = Map[Prefix, (Prefix, ListMap[Option[Variable], Option[Variable]])]()

	private val seqMgr = new SequenceManager(new SequenceExecutor(stats, config), config, finalizer)

	private val global = new GlobalState(config, typeProvider, seqMgr, stats, random, finalizer, classLoaderV1, classLoaderV2, focusMethods)

	private val noFocusMethodsFound = focusMethods match {
		case Some(map) => map.isEmpty
		case _ => true
	}

	private val tsOracle = new TSOracleNormalExec(finalizer, stats, seqMgr.seqExecutor, config, global)

	// hack to always go through the PUT class loader -- ideally TypeResolver would be a class instead of a singleton 
	TypeResolver.bcReader.classLoader = classLoaderV0

	def run: Unit = {
		val dlMonitor = new DeadlockMonitor(config, global)
		dlMonitor.setDaemon(true)
		dlMonitor.start
		
		var precisionString = "target stdd: " + (config.targetThresholdQuiet * 100)  + "%, acceptable stdd: " + (config.acceptableThresholdQuiet * 100) + "%"
		if (config.noisyEnvironment) {
			precisionString = "target stdd: " + (config.targetThresholdNoisy * 100)  + "%, acceptable stdd: " + (config.acceptableThresholdNoisy * 100) + "%"
		}
		println("Running Test Suite with " + precisionString)

		var abortTestRun = tuningParameters
		
		println()
		println("cutCallsPerSeq : " + config.cutCallsPerSeq)
		println("maxSuffixLength: " + config.maxSuffixLength)
		println("suffixRuns     : " + config.suffixRuns)
		
		// reset the random generator
		random.reset

		println()

		if (!abortTestRun) {
			generateAndRunTests
		}

		println("Stopping the dlMonitor")
		dlMonitor.interrupt()
		dlMonitor.stop()
		try {
			dlMonitor.join()
		} catch {
			case e: InterruptedException => e.printStackTrace()
		}
		println("ClassTester: Reached end of run()")
	}

	private def tuningParameters: Boolean = {
		println("Tuning Parameters")
		var suffixLengthFound = false

		val nrThreadsMin = config.nrThreads.first
		val nrThreadsMax = config.nrThreads.last
		var tries = 0
		var exceptions = 0
		println("running time tests with " + nrThreadsMin + " (min) and " + nrThreadsMax + " (max) threads")

		val minMax = new MinMax()

		var optimumCanBeReached = true
		val maxTries = 10
		var abortTestRun = false
		//val exceptionThreashold = (maxTries / 2)
		val exceptionThreashold = maxTries

		var lowerCutCallsPerSeq = 1
		var upperCutCallsPerSeq = 200
		var lowerSuffixRuns = 1
		var upperSuffixRuns = 400
		
		val targetMin = 50
		val targetMax = 2000
		var subsequentTimeouts = 0
		
		val indent = "   "

		config.cutCallsPerSeq = (lowerCutCallsPerSeq + upperCutCallsPerSeq) / 2
		config.maxSuffixLength = config.cutCallsPerSeq * 4
		config.suffixRuns = (lowerSuffixRuns + upperSuffixRuns) / 2


		// tuning the suffix length and the suffix runs
		while (!abortTestRun && !suffixLengthFound && tries < maxTries) {
			val (prefix, prefixV1, prefixV2, var2varV1, var2varV2) = getPrefixes
			val suffixGen = prefix2SuffixGen(prefix)
			
			tries += 1
			println(indent + "trying cutCallsPerSeq : " + config.cutCallsPerSeq)
			println(indent + "trying maxSuffixLength: " + config.maxSuffixLength)
			println(indent + "trying suffixRuns     : " + config.suffixRuns)
			println(indent + "generating suffix...")
			
			var nextSuffixOpt = suffixGen.nextSuffix(config.cutCallsPerSeq, config.maxSuffixLength)
			nextSuffixOpt match {
				case Some(suffix) => {
					minMax.reset
					var (suffixV1, successV1) = suffix.adaptToNewClassVersion(prefixV1, var2varV1.clone, global.classLoaderV1)
					var (suffixV2, successV2) = suffix.adaptToNewClassVersion(prefixV2, var2varV2.clone, global.classLoaderV2)
					if (successV1 && successV2) {
						minMax.set(tsOracle.getRepsCount(1, prefixV1, suffixV1, suffixV1, config.suffixRuns, nrThreadsMin, config.runningTimeMultipliers(config.nrThreads.indexOf(nrThreadsMin))))
						minMax.set(tsOracle.getRepsCount(2, prefixV2, suffixV2, suffixV2, config.suffixRuns, nrThreadsMin, config.runningTimeMultipliers(config.nrThreads.indexOf(nrThreadsMin))))
						if (minMax.min > 0) {
							minMax.set(tsOracle.getRepsCount(1, prefixV1, suffixV1, suffixV1, config.suffixRuns, nrThreadsMax, config.runningTimeMultipliers(config.nrThreads.indexOf(nrThreadsMax))))
							minMax.set(tsOracle.getRepsCount(2, prefixV2, suffixV2, suffixV2, config.suffixRuns, nrThreadsMax, config.runningTimeMultipliers(config.nrThreads.indexOf(nrThreadsMax))))
						} else {
							println(indent + "there were exceptions, skipping for " + nrThreadsMax + " threads")
						}
					} else {
						minMax.set(-1)
					}

					if (minMax.min == -2) {
						subsequentTimeouts += 1
						if (minMax.timeouts >= 2 || subsequentTimeouts >= 2) {
							subsequentTimeouts = 0
							println(indent + "two ore more test runs or two subsequent runs have timed out - trying shorter sequences")
							if (upperCutCallsPerSeq > (config.cutCallsPerSeq + 5)) {
								upperCutCallsPerSeq = config.cutCallsPerSeq
								upperSuffixRuns = config.suffixRuns
								config.cutCallsPerSeq = (lowerCutCallsPerSeq + upperCutCallsPerSeq) / 2
								config.maxSuffixLength = config.cutCallsPerSeq * 4
								config.suffixRuns = (lowerSuffixRuns + upperSuffixRuns) / 2
							} else {
								optimumCanBeReached = false
								suffixLengthFound = true
								println(indent + "we won't get any closer to the optimum, giving up")
							}
						} else {
							println(indent + "at least one of the test runs timed out - skipping")
							exceptions += 1
							if (exceptions >= exceptionThreashold) {
								abortTestRun = true
							}
						}
					} else if (minMax.min == -1) {
						subsequentTimeouts = 0
						println(indent + "there was an exception - skipping")
						exceptions += 1
						if (exceptions > exceptionThreashold) {
							abortTestRun = true
						}

					} else {
						subsequentTimeouts = 0
						println(indent + "min repetitions: " + minMax.min)
						println(indent + "max repetitions: " + minMax.max)

						var tooFew = false
						var tooMany = false

						if (minMax.min < targetMin) {
							println(indent + "-> too few!")
							tooFew = true
						}
						if (optimumCanBeReached && minMax.max > targetMax) {
							println(indent + "-> too many!")
							tooMany = true
						}

						if (!tooFew && !tooMany) {
							suffixLengthFound = true
						} else {
							if (tooFew) {
								if (tooMany) {
									// we won't reach our goal, but we want at least satisfy the mininum repetition
									optimumCanBeReached = false
								}
								// must be too few, decrease the cut calls and the suffix runs
								if (upperCutCallsPerSeq > (config.cutCallsPerSeq + 5)) {
									upperCutCallsPerSeq = config.cutCallsPerSeq
									upperSuffixRuns = config.suffixRuns
								} else {
									optimumCanBeReached = false
									suffixLengthFound = true
									println(indent + "we won't get any closer to the optimum, giving up")
								}
							} else {
								// must be too many
								// increase the cut calls and the suffix runs
								if (lowerCutCallsPerSeq < (config.cutCallsPerSeq - 5)) {
									lowerCutCallsPerSeq = config.cutCallsPerSeq
									lowerSuffixRuns = config.suffixRuns
								} else {
									optimumCanBeReached = false
									suffixLengthFound = true
									println(indent + "we won't get any closer to the optimum, giving up")
								}
							}
						}

						if (!suffixLengthFound) {
							config.cutCallsPerSeq = (lowerCutCallsPerSeq + upperCutCallsPerSeq) / 2
							config.maxSuffixLength = config.cutCallsPerSeq * 4
							config.suffixRuns = (lowerSuffixRuns + upperSuffixRuns) / 2
						}
					}
				}
				case None => {
					println(indent + "could not generate a suffix")
					exceptions += 1
				}
			}
		}

		if (abortTestRun) {
			println(" --- could not tune the parameters because there were too many exceptions, aborting the test run!")
			println(" --- there were " + exceptions + " exceptions and " + tries + " tries. (threashold was: " + exceptionThreashold + ")")
		} else {
			println(" --- Accepting parameters, starting Tests.")
		}

		return abortTestRun
	}

	private def generateAndRunTests() {
		var validTests = 0
		var suffixGenTries = 0
		var generatedSuffixes = 0

		while (suffixGenTries < config.maxSuffixGenTries && validTests < 10) {
			suffixGenTries += 1
			println()
			println("suffixGenTries   : " + suffixGenTries)
			println("generatedSuffixes: " + generatedSuffixes)
			println("maxSuffixGenTries: " + config.maxSuffixGenTries)
			println("validTests       : " + validTests)
			println()

			stats.timer.start("gen")
			val (prefix, prefixV1, prefixV2, var2varV1, var2varV2) = getPrefixes
			stats.timer.stop("gen")

			val suffixGen = prefix2SuffixGen(prefix)
			val suffixes = prefix2Suffixes(prefix)

			val suffixesV1 = prefix2Suffixes(prefixV1)
			val suffixesV2 = prefix2Suffixes(prefixV2)

			var nextSuffixOpt = suffixGen.nextSuffix(config.cutCallsPerSeq, config.maxSuffixLength)

			nextSuffixOpt match {
				case Some(suffix) => {
					generatedSuffixes += 1
					assert(suffix.length > 0)
					if (!suffixes.exists(oldSuffix => suffix.equivalentTo(oldSuffix))) {
						var (suffixV1, successV1) = suffix.adaptToNewClassVersion(prefixV1, var2varV1.clone, global.classLoaderV1)
						var (suffixV2, successV2) = suffix.adaptToNewClassVersion(prefixV2, var2varV2.clone, global.classLoaderV2)
						
						if (successV1 && successV2) {
							suffixes += suffix
							suffixesV1 += suffixV1
							suffixesV2 += suffixV2
	
							var suffixTestRuns = 0
	
							var v1First = global.random.nextBool
								
							for (i <- 0 until suffixes.length) {
								suffixTestRuns += 1
	
								val otherSuffix = suffixes(i)
								val otherSuffixV1 = suffixesV1(i)
								val otherSuffixV2 = suffixesV2(i)
	
								if (config.debugLevel > 1) {
									println(">>>>>>>>>>>>>")
									println(suffix.toString())
									println("<<<<<<<<<<<<<")
									println(otherSuffix.toString())
									println(">>>>>>>>>>>>>")
								}
	
								var threadRun = 0
								var valid = true
	
								config.nrThreads.foreach(nrThreads => {
									threadRun += 1
									if (!valid) {
										println("There were exceptions or timeout with fewer threads, skipping " + nrThreads)
									} else {
										println()
										println("Generated prefixes        : " + prefix2SuffixGen.keySet.size)
										println("Generated suffixes (curr) : " + suffixes.size)
										println("Generated suffixes (total): " + generatedSuffixes)
										println("Suffix Test Runs          : " + suffixTestRuns + " of " + suffixes.length)
										println("Run                       : " + threadRun + " (" + nrThreads + " threads) of " + config.nrThreads.length)
										println("Valid Tests (total)       : " + validTests)
										println()
	
										var runId = MurmurHash.stringHash(prefixV1.toString).toString
										runId += "-" + MurmurHash.stringHash(suffixV1.toString).toString
										runId += "-" + MurmurHash.stringHash(otherSuffixV1.toString).toString
										runId += "#" + nrThreads
										val multiplier = config.runningTimeMultipliers(config.nrThreads.indexOf(nrThreads))
										var rep_v1 = tsOracle.getRepsCount(1, prefixV1, suffixV1, otherSuffixV1, config.suffixRuns, nrThreads, multiplier)
										var rep_v2 = tsOracle.getRepsCount(2, prefixV2, suffixV2, otherSuffixV2, config.suffixRuns, nrThreads, multiplier)
										var repetitions = 0
										if (rep_v1 < 0 || rep_v2 < 0) {
											println(" --- there were exceptions, setting repetitions to 0")
											valid = false
										} else if (rep_v1 < 10 || rep_v2 < 10) {
											valid = false
											println(" --- too few repetitions in warm-up, setting repetitions to 0")
										} else {
											repetitions = (rep_v1 + rep_v2) / 2
											println(" --- setting number of repetitions to " + repetitions)
											println()
											
											val runID = generateRunID(prefix, suffix, otherSuffix)
	
											var (v1, v2) = if (v1First) {
												val tmp1 = tsOracle.analyzeTestForPerformance(1, prefixV1, suffixV1, otherSuffixV1, runID, config.suffixRuns, nrThreads, repetitions, multiplier)
												val tmp2 = tsOracle.analyzeTestForPerformance(2, prefixV2, suffixV2, otherSuffixV2, runID, config.suffixRuns, nrThreads, repetitions, multiplier)
												(tmp1, tmp2)
											} else {
												val tmp2 = tsOracle.analyzeTestForPerformance(2, prefixV2, suffixV2, otherSuffixV2, runID, config.suffixRuns, nrThreads, repetitions, multiplier)
												val tmp1 = tsOracle.analyzeTestForPerformance(1, prefixV1, suffixV1, otherSuffixV1, runID, config.suffixRuns, nrThreads, repetitions, multiplier)
												(tmp1, tmp2)
											}
	
											println()
											println("*" * 30)
											println("test run : " + runID)
											println("v1       : " + v1 + " milliseconds")
											println("v2       : " + v2 + " milliseconds")
											if (v1 > 0 && v2 > 0) {
												var ratio = if (v1 < v2) {
													(v2.toDouble / v1.toDouble) - 1
												} else {
													(v1.toDouble / v2.toDouble) - 1
												}
												println("v_ratio  : " + ratio)
												validTests += 1
											} else {
												println("one or both versions produced invalid results")
											}
											println("*" * 30)
											println()
										}
									}
								})
								
								v1First = !v1First
							}
						} else {
							println("\n --- could not adapt the suffix to one of the versioned suffixes, skipping and generating another suffix")
						}
					}
				}
				case None => println("could not generate a suffix")
			}
		}
	}
	
	private def generateRunID(prefix: Prefix, suffix1: Suffix, suffix2: Suffix): String = {
		var returnValue = MurmurHash.stringHash(prefix.toString).toString
		returnValue += "-" + MurmurHash.stringHash(suffix1.toString).toString
		returnValue += "-" + MurmurHash.stringHash(suffix2.toString).toString
		return returnValue
	}
	
	private def getPrefixes: (Prefix, Prefix, Prefix, ListMap[Option[Variable], Option[Variable]], ListMap[Option[Variable], Option[Variable]]) = {
		if (prefixes.size < config.maxPrefixes) { // try to create a new prefix
			new InstantiateCutTask(global).run match {
				case Some(prefix) => {
					prefix.fixCutVariable
					assert(prefix.types.contains(config.cut), prefix.types)
					if (prefixes.exists(oldPrefix => prefix.equivalentTo(oldPrefix))) { // we re-created an old prefix, return a random existing prefix 
						var prefix = prefixes(global.random.nextInt(prefixes.size))
						val (prefixV1, var2varV1) = prefix2PrefixV1(prefix)
						val (prefixV2, var2varV2) = prefix2PrefixV2(prefix)
						return (prefix, prefixV1, prefixV2, var2varV1, var2varV2)
					} else {
						val extendedPrefix = appendStateChangers(prefix)
						val (prefixV1, var2varV1, successV1) = extendedPrefix.adaptToNewClassVersion(global.classLoaderV1)
						val (prefixV2, var2varV2, successV2) = extendedPrefix.adaptToNewClassVersion(global.classLoaderV2)
						if (successV1 && successV2) {
							prefixes.add(extendedPrefix)
							prefix2SuffixGen.put(extendedPrefix, new SuffixGen(extendedPrefix, global, config))
							prefix2Suffixes.put(extendedPrefix, new ArrayList[Suffix]())
							prefix2PrefixV1(extendedPrefix) = (prefixV1, var2varV1)
							prefix2PrefixV2(extendedPrefix) = (prefixV2, var2varV2)
							prefix2Suffixes.put(prefixV1, new ArrayList[Suffix]())
							prefix2Suffixes.put(prefixV2, new ArrayList[Suffix]())	
							return (extendedPrefix, prefixV1, prefixV2, var2varV1, var2varV2)	
						} else {
							println(" >>> WARNING: could not adapt prefix to one of the versions")
							println(" >>> Trying to use an old prefix instead")
							if (prefixes.isEmpty) {
								println("Could not adapt any prefix to for the versioned prefixes.")
								finalizer.finalizeAndExit(false)
								return null
							} else {
								var prefix = prefixes(global.random.nextInt(prefixes.size))
								val (prefixV1, var2varV1) = prefix2PrefixV1(prefix)
								val (prefixV2, var2varV2) = prefix2PrefixV2(prefix)
								return (prefix, prefixV1, prefixV2, var2varV1, var2varV2)
							}
						}
					}
				}
				case None => {
					if (prefixes.isEmpty) {
						println("Cannot instantiate " + config.cut + ". Is there a public constructor or method to create it?")
						finalizer.finalizeAndExit(false)
						return null
					} else {
						var prefix = prefixes(global.random.nextInt(prefixes.size))
						val (prefixV1, var2varV1) = prefix2PrefixV1(prefix)
						val (prefixV2, var2varV2) = prefix2PrefixV2(prefix)
						return (prefix, prefixV1, prefixV2, var2varV1, var2varV2)
					}
				}
			}
		} else {
			var prefix = prefixes(global.random.nextInt(prefixes.size))
			val (prefixV1, var2varV1) = prefix2PrefixV1(prefix)
			val (prefixV2, var2varV2) = prefix2PrefixV2(prefix)
			return (prefix, prefixV1, prefixV2, var2varV1, var2varV2)
		}
	}

	private def appendStateChangers(prefix: Prefix) = {
		var currentSequence = prefix
		for (_ <- 1 to config.maxStateChangersInPrefix) {
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
}

object ClassTester extends Finalizer {

	val startTime = System.currentTimeMillis
	var stats: Stats = _
	var config: Config = new Config(new File("/tmp/"))

	def main(args: Array[String]): Unit = {
		println("Starting ClassTester at " + new Date() + " version 0.51")

		val dateFormat = new java.text.SimpleDateFormat("yyyyMMddHHmm")
		// default values for the optional parameters, empty values for mandatory arguments
		case class OptionsConfig(
			cut: String = "",
			resultsBasePath: String = "",
			focusMethodsFileName: String = "",
			excludedMethodsFileName: String = "",
			jarsDirectory: String = "",
			envTypesString: String = "",
			seed: Int = 0,
			projectId: String = "project_id",
			revisions: String = "v1,v2",
			dateString: String = dateFormat.format(new java.util.Date()).toString,
			debugLevel: Int = 0,
			noisy: Boolean = false)

		// set some default values directly
		config.callClinit = false
		//config.cutCallsPerSeq = 20
		//config.maxSuffixLength = 140
		//config.suffixRuns = 20
		config.maxStateChangersInPrefix = 5
		config.chooseCUTCall = 2

		val parser = new scopt.immutable.OptionParser[OptionsConfig]("ClassTester", "0.7") {
			def options = Seq(
				// arguments
				arg("<cut>", "Class under Test") { (v: String, c: OptionsConfig) => c.copy(cut = v) },
				arg("<jarsDirectory>", "String with the jar directory (jars should be in the form <project_id>_<revision_v1>_<revision_v2>_v[1|2].jar)") { (v: String, c: OptionsConfig) => c.copy(jarsDirectory = v) },
				arg("<results_base_path>", "Base path to write the results to.") { (v: String, c: OptionsConfig) => c.copy(resultsBasePath = v) },
				arg("<project_id", "The project ID, used for identification") { (v: String, c: OptionsConfig) => c.copy(projectId = v) },
				arg("<revisions", "Revision of version 1 and version 2 (eg. 'r1234,r1235')") { (v: String, c: OptionsConfig) => c.copy(revisions = v) },
				// optional arugments (with default values above)
				opt("d", "date", "Date string for writing files") { (v: String, c: OptionsConfig) => c.copy(dateString = v) },
				opt("fms", "focus-methods", "File with focus methods") { (v: String, c: OptionsConfig) => c.copy(focusMethodsFileName = v) },
				opt("ems", "excluded-methods", "File with methods to exclude from test generation") { (v: String, c: OptionsConfig) => c.copy(excludedMethodsFileName = v) },
				opt("ets", "env-types-string", "String with env types") { (v: String, c: OptionsConfig) => c.copy(envTypesString = v) },
				intOpt("s", "seed", "Seed for random number generator") { (v: Int, c: OptionsConfig) => c.copy(seed = v) },
				intOpt("db", "debug-level", "Debug Level from 0 (nothing) to 3 (max)") { (v: Int, c: OptionsConfig) => c.copy(debugLevel = v) },
				booleanOpt("n", "noisy", "Noisy environment") { (v: Boolean, c: OptionsConfig) => c.copy(noisy = v) })
		}

		// config paramaters that are just locally used in this method
		var basePath = ""
		var performanceResultsPath = ""
		var dataPath = ""
		var resultsPath = ""
		var dateString = ""
		var focusMethodsFileName = ""
		var excludedMethodsFileName = ""
		var jarsDirectory = ""
		var envTypesString = ""

		// parse the arguments, set the config values
		parser.parse(args, OptionsConfig()) map { cfg =>
			var projectId = "project"
			config.projectId = cfg.projectId
			config.cut = cfg.cut
			config.host = java.net.InetAddress.getLocalHost().getHostName()
			config.noisyEnvironment = cfg.noisy
			basePath = cfg.resultsBasePath
			performanceResultsPath = basePath + "/performance_results"
			resultsPath = basePath + "/results"
			dataPath = basePath + "/data"
			dateString = cfg.dateString
			jarsDirectory = cfg.jarsDirectory
			envTypesString = cfg.envTypesString

			val revisions = cfg.revisions.split(",")
			config.revisionV1 = revisions(0)
			config.revisionV2 = revisions(1)

			focusMethodsFileName = cfg.focusMethodsFileName
			excludedMethodsFileName = cfg.excludedMethodsFileName

			val performanceSummaryFileName = dateString + "_" + config.projectId + "_" + config.revisionV1 + "_" + config.revisionV2 + ".txt"
			val dataFileName = dateString + "_" + config.projectId + "_" + config.revisionV1 + "_" + config.revisionV2 + ".txt"
			config.setPerformanceSummaryWriter(performanceResultsPath, performanceSummaryFileName)
			config.seed = cfg.seed

			config.debugLevel = cfg.debugLevel
		} getOrElse {
			exit(0)
		}

		// determine the number of available processors and the number of threads the test should be run with
		val cores = if (Runtime.getRuntime().availableProcessors() > 1) {
			Runtime.getRuntime().availableProcessors()
		} else {
			2
		}
		
//		config.nrThreads = Array(cores, cores * 8)  // for real users of the tool: nb of threads depends on available cores
		config.nrThreads = Array(8, 64) // for the evaluation: fix the number of threads to 8 and 64 

		// how long should the tests run for the number of threads
		config.runningTimeMultipliers = Array(2, 4)
		
		var random = new Random(config.seed)

		stats = new Stats(config)

		println("Testing " + config.cut + " with seed " + config.seed)
		config.performanceSummaryWriter.appendResultMsg("Testing " + config.cut + " with seed " + config.seed)
		stats.timer.start("all")

		var jarV0 = jarsDirectory + "/" + config.projectId + "_" + config.revisionV1 + "_" + config.revisionV2 + "_v0.jar"
		var jarV1 = jarsDirectory + "/" + config.projectId + "_" + config.revisionV1 + "_" + config.revisionV2 + "_v1.jar"
		var jarV2 = jarsDirectory + "/" + config.projectId + "_" + config.revisionV1 + "_" + config.revisionV2 + "_v2.jar"

		println(jarV0)
		println(jarV1)
		println(jarV2)

		val classLoaderV0 = new CustomClassLoader(this.getClass().getClassLoader, new URLClassLoader(Array[URL](new File(jarV0).toURI.toURL)))
		val classLoaderV1 = new CustomClassLoader(this.getClass().getClassLoader, new URLClassLoader(Array[URL](new File(jarV1).toURI.toURL)))
		val classLoaderV2 = new CustomClassLoader(this.getClass().getClassLoader, new URLClassLoader(Array[URL](new File(jarV2).toURI.toURL)))

		if (envTypesString.size > 0) {
			envTypesString += ":java.lang.Object"
		} else {
			envTypesString = "java.lang.Object"
		}

		var excludedMethods = Util.loadExcludedMethodsFromFile(excludedMethodsFileName)

		val typeProvider = new TypeManager(config.cut, envTypesString.split(":"), classLoaderV0, classLoaderV1, classLoaderV2, random, config, excludedMethods)

		// try to load the focus methods
		var focusMethods = Some(Util.loadFocusMethodsFromFile(focusMethodsFileName))

		// if there was no focus methods file try to determine the focus methods by analyzing the jars
		val finalFocusMethods: Option[scala.collection.immutable.Map[String, Int]] = focusMethods match {
			case Some(map) => {
				if (map.isEmpty) {
					// analyze the jars
					var mchecker = new MethodCheck(jarV1, jarV2, config.cut)
					mchecker.check
					// get the methods that have changed
					val differingMethods = mchecker.getDifferingMethods()
					typeProvider.cutMethods.foreach(method => {
						// match the methods that have changed and add them to the focus methods
						if (differingMethods.contains(method.methodName)) {
							//TODO: Not implemented
							map(method.toString()) = 20
						}
					})
				}
				Some(map.toMap[String, Int])
			}
			case _ => Some(new scala.collection.immutable.ListMap[String, Int]())
		}

		println("Focusing on the following methods with priorities:")
		focusMethods match {
			case Some(map) => map.foreach { case (key, value) => println(key + ": " + value) }
			case _ => println("no focus methods found")
		}

		val tester = new ClassTester(config, stats, classLoaderV0, classLoaderV1, classLoaderV2, ".", random, this, finalFocusMethods, typeProvider)
		tester.run
		stats.performanceTimer.write(config)

		finalizeAndExit(false)
	}

	def finalizeAndExit(bugFound: Boolean) = {
		stats.timer.stop("all")
		stats.print
		stats.timer.print
		println("Done with ClassTester at " + new Date)
		val secondsTaken = (System.currentTimeMillis - startTime) / 1000
		println("Time (seconds): " + secondsTaken)

		config.performanceListeners.foreach(_.notifyDone)

		config.performanceSummaryWriter.write

		System.exit(0)
	}

	private def readMethods(fileName: String) = {
		val result = new ArrayList[String]()
		val r = new BufferedReader(new FileReader(fileName))
		var line = r.readLine
		while (line != null) {
			result.add(line)
			line = r.readLine
		}
		r.close
		result
	}

}

class MinMax {
	var min: Int = Int.MaxValue
	var max: Int = 0
	var timeouts: Int = 0

	def reset() {
		min = Int.MaxValue
		max = 0
		timeouts = 0
	}

	def set(value: Int) {
		if (value == -2) {
			timeouts += 1
		}
		if (value < min) {
			min = value
		}
		if (value > max) {
			max = value
		}
	}
}
