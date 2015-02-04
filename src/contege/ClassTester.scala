package contege

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.FileReader
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.Date
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import contege.seqexec.DeadlockMonitor
import contege.seqgen.InstantiateCutTask
import contege.seqgen.Prefix
import contege.seqgen.StateChangerTask
import contege.seqgen.Suffix
import contege.seqgen.SuffixGen
import contege.seqgen.TypeManager
import javamodel.util.TypeResolver
import contege.seqexec.TestPrettyPrinter
import contege.seqexec.jpf.JPFFirstSequenceExecutor
import contege.seqexec.jpf.TSOracleJPFFirst
import java.io.File
import contege.seqexec.reflective.TSOracleNormalExec
import contege.seqexec.reflective.SequenceManager
import contege.seqexec.reflective.SequenceExecutor

class ClassTester(config: Config, stats: Stats, putClassLoader: ClassLoader, putJarPath: String, envTypes: ArrayList[String],
    random: Random, finalizer: Finalizer) {

    // PLDI 2012
    private val cutCallsPerSeq = 2
    private val maxSuffixLength = 10
    private val concRunRepetitions = 100
    private val maxStateChangersInPrefix = 5

//    private val cutCallsPerSeq = 2
//    private val maxSuffixLength = 10
//    private val concRunRepetitions = 1000
//    private val maxStateChangersInPrefix = 5

    private val prefixes = new ArrayList[Prefix] // kept separately to ensure deterministic random selection of one
    private val prefix2SuffixGen = Map[Prefix, SuffixGen]()
    private val prefix2Suffixes = Map[Prefix, ArrayList[Suffix]]()

    private val typeProvider = new TypeManager(config.cut, envTypes, putClassLoader, random)

    private val seqMgr = new SequenceManager(new SequenceExecutor(stats, config), config, finalizer)

    private val global = new GlobalState(config, typeProvider, seqMgr, stats, random, finalizer)

    private val tsOracle = if (config.useJPFFirst) {
        val jpfFirstExecutor = new JPFFirstSequenceExecutor(global, putJarPath)
        new TSOracleJPFFirst(finalizer, stats, config, seqMgr.seqExecutor, jpfFirstExecutor)
    } else {
        new TSOracleNormalExec(finalizer, concRunRepetitions, stats, seqMgr.seqExecutor, config)
    }

    // hack to always go through the PUT class loader -- ideally TypeResolver would be a class instead of a singleton 
    TypeResolver.bcReader.classLoader = putClassLoader

    def run: Unit = {
        val dlMonitor = new DeadlockMonitor(config, global)
        dlMonitor.setDaemon(true)
        dlMonitor.start

        var nbGeneratedTests = 0L
        config.checkerListeners.foreach(l => l.updateNbGeneratedTests(nbGeneratedTests))

        for (suffixGenTries <- 1 to config.maxSuffixGenTries) { // generate call sequences
            stats.timer.start("gen")
            val prefix = getPrefix
            stats.timer.stop("gen")

            val suffixGen = prefix2SuffixGen(prefix)
            val suffixes = prefix2Suffixes(prefix)

            stats.timer.start("gen")
            val nextSuffixOpt = suffixGen.nextSuffix(cutCallsPerSeq)
            stats.timer.stop("gen")
            nextSuffixOpt match {
                case Some(suffix) => {
                    assert(suffix.length > 0)
                    if (!suffixes.exists(oldSuffix => suffix.equivalentTo(oldSuffix))) {
                        suffixes += suffix
                        println("New suffix \n" + suffix)
                        // run the new sequences against all existing sequences and again itself
                        suffixes.foreach(otherSuffix => {
                            nbGeneratedTests += 1
                            config.checkerListeners.foreach(l => l.updateNbGeneratedTests(nbGeneratedTests))
                            println("Nb generated tests: " + nbGeneratedTests)

                            finalizer.currentTest = Some(TestPrettyPrinter.javaCodeFor(prefix, suffix, otherSuffix, "GeneratedTest", TestPrettyPrinter.NoOutputVectors))

                            tsOracle.analyzeTest(prefix, suffix, otherSuffix)
                        })
                    }
                    if (suffixes.size % 5 == 0) {
                        stats.print
                        stats.timer.print
                    }
                }
                case None => // ignore
            }
        }

        println("ClassTester: Could not find bug.")	
    }

    private def getPrefix: Prefix = {
        if (prefixes.size < config.maxPrefixes) { // try to create a new prefix
            new InstantiateCutTask(global).run match {
                case Some(prefix) => {
                    prefix.fixCutVariable
                    assert(prefix.types.contains(config.cut), prefix.types)
                    if (prefixes.exists(oldPrefix => prefix.equivalentTo(oldPrefix))) { // we re-created an old prefix, return a random existing prefix 
                        return prefixes(random.nextInt(prefixes.size))
                    } else {
                        val extendedPrefix = appendStateChangers(prefix)
                        prefixes.add(extendedPrefix)
                        prefix2SuffixGen.put(extendedPrefix, new SuffixGen(extendedPrefix, maxSuffixLength, global))
                        prefix2Suffixes.put(extendedPrefix, new ArrayList[Suffix]())
                        println("New prefix:\n" + extendedPrefix)
                        return extendedPrefix
                    }
                }
                case None => {
                    if (prefixes.isEmpty) {
                        config.checkerListeners.foreach(_.appendResultMsg("Cannot instantiate " + config.cut + ". Is there a public constructor or method to create it?"))
                        finalizer.finalizeAndExit(false)
                        return null
                    } else prefixes(random.nextInt(prefixes.size))
                }
            }
        } else {
            return prefixes(random.nextInt(prefixes.size))
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
}

object ClassTester extends Finalizer {

    val startTime = System.currentTimeMillis
    var stats: Stats = _
    var config: Config = _

    def main(args: Array[String]): Unit = {
        println("Starting ClassTester at " + new Date())
        assert(args.size == 6 || args.size == 7)
        val cut = args(0)

        val seed = args(2).toInt
        val maxSuffixGenTries = args(3).toInt
        assert(maxSuffixGenTries >= 2)
        val callClinit = args(5).toBoolean
        val selectedCUTMethods: Option[ArrayList[String]] = if (args.size == 7) Some(readMethods(args(6))) else None
        if (selectedCUTMethods.isDefined) println("Focusing on "+selectedCUTMethods.get.size+" CUT methods")
        else println("No specific CUT methods selected")
        config = new Config(cut, seed, maxSuffixGenTries, selectedCUTMethods, new File("/tmp/"), callClinit)
        val random = new Random(seed)

        val resultFileName = args(4)
        config.addCheckerListener(new ResultFileCheckerListener(resultFileName))

        val envTypes = new ArrayList[String]
        envTypes.add("java.lang.Object")
        Util.addEnvTypes(args(1), envTypes, this.getClass.getClassLoader)

        stats = new Stats

        val tester = new ClassTester(config, stats, getClass.getClassLoader, ".", envTypes, random, this)
        println("Testing " + cut + " with seed " + seed)
        stats.timer.start("all")

        tester.run

        finalizeAndExit(false)
    }

    def finalizeAndExit(bugFound: Boolean) = {
        stats.timer.stop("all")
        stats.print
        stats.timer.print
        println("Done with ClassTester at " + new Date)
        val secondsTaken = (System.currentTimeMillis - startTime) / 1000
        config.checkerListeners.foreach(_.appendResultMsg("Time (seconds): " + secondsTaken))

        if (bugFound) {
            val testCode = currentTest.get
            config.checkerListeners.foreach(_.notifyDoneAndBugFound(testCode))
        } else config.checkerListeners.foreach(_.notifyDoneNoBug)

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
