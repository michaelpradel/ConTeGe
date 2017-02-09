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
import cfp.PotentialCFPs
import cfp.NextCFP
import cfp.CFPDetection
import java.util.Calendar
import contege.seqgen.TypeManager
import contege.seqexec.reflective.SequenceManager

class ClassTester(config: Config, stats: Stats, putClassLoader: ClassLoader, putJarPath: String, envTypes: ArrayList[String],
  random: Random, finalizer: Finalizer, cutMethodsToTest1: Seq[MethodAtom], cutMethodsToTest2: Seq[MethodAtom], seed: Int, seedPrefix: Map[String, Prefix], typeProvider: TypeManager, cutCallsPerSeq : Int) {
  
  private val maxSuffixLength = 10
  private val concRunRepetitions = 1
  private val nbGenTestPerNextCFP = 2
  private var maxStateChangersInPrefix = 0
  var prefixGenerated: Prefix = null
  

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

    var nbGeneratedTests = 0L
    config.checkerListeners.foreach(l => l.updateNbGeneratedTests(nbGeneratedTests))

    for (suffixGenTries <- 1 to nbGenTestPerNextCFP) { // generate call sequences

      if (suffixGenTries == 1) {
        maxStateChangersInPrefix = 0
        println("StateChanger:" + maxStateChangersInPrefix)
      } else {
        maxStateChangersInPrefix = 5
        println("StateChanger:" + maxStateChangersInPrefix)
      }

      stats.timer.start("gen")
      val prefix = getPrefix
      stats.timer.stop("gen")

      if (prefix == null) {
        return
      }

      stats.timer.start("gen")
      val suffixGen = new SuffixGen(prefix, maxSuffixLength, global)
      stats.timer.start("gen")

      stats.timer.start("gen")
      val nextSuffixOpt = suffixGen.nextSuffix(cutCallsPerSeq, cutMethodsToTest1)
      stats.timer.stop("gen")
      nextSuffixOpt match {
        case Some(suffix) => {

          assert(suffix.length > 0)

          stats.timer.start("gen")
          val nextSuffixOpt = suffixGen.nextSuffix(cutCallsPerSeq, cutMethodsToTest2)
          stats.timer.stop("gen")
          nextSuffixOpt match {
            case Some(otherSuffix) => {

              assert(otherSuffix.length > 0)
              println("Suffix 1 :" + suffix)
              println("Suffix 2 :" + otherSuffix)
              nbGeneratedTests += 1
              stats.genTests.incr
              config.checkerListeners.foreach(l => l.updateNbGeneratedTests(nbGeneratedTests))
              println("Nb generated tests: " + nbGeneratedTests)
              finalizer.currentTest = Some(TestPrettyPrinter.javaCodeFor(prefix, suffix, otherSuffix, "GeneratedTest", TestPrettyPrinter.NoOutputVectors))

              tsOracle.analyzeTest(prefix, suffix, otherSuffix)

              stats.timer.start("cfp_det")
              val cfpDetection = new CFPDetection();
              cfpDetection.detectCFP("Instrument", "Instrument_Traces");
              stats.timer.stop("cfp_det")

              //stats.timer.print_new(NextCFP.nextCFPMethod1 + NextCFP.nextCFPMethod2)
            }
            case None => //ignore
          }
        }
        case None => //ignore
      }
    }

    println("ClassTester: Could not find bug.")
  }

  private def getPrefix: Prefix = {
    if (seedPrefix.contains("" + seed + maxStateChangersInPrefix)) {
      prefixGenerated = seedPrefix("" + seed + maxStateChangersInPrefix)
      println("Prefix:\n" + prefixGenerated)
      return prefixGenerated
    } else if (maxStateChangersInPrefix == 0) {
      new InstantiateCutTask(global).run match {
        case Some(prefix) => {
          prefix.fixCutVariable
          assert(prefix.types.contains(config.cut), prefix.types)
          prefixGenerated = prefix
          seedPrefix.put("" + seed + maxStateChangersInPrefix, prefixGenerated)
          println("Prefix:\n" + prefixGenerated)
          return prefixGenerated
        }
        case None => {
          config.checkerListeners.foreach(_.appendResultMsg("Cannot instantiate " + config.cut + ". Is there a public constructor or method to create it?"))
          seedPrefix.put("" + seed + maxStateChangersInPrefix, null)
          return null
        }
      }
    } else {
      prefixGenerated = appendStateChangers(seedPrefix("" + seed + 0))
      seedPrefix.put("" + seed + maxStateChangersInPrefix, prefixGenerated)
      println("Prefix:\n" + prefixGenerated)
      return prefixGenerated
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
  val seedPrefix = Map[String, Prefix]()
  val seedTypeProviderMap = Map[Integer, TypeManager]()
  val seedRandomMap = Map[Integer, Random]()

  private val concRunRepetitions = 1
  def main(args: Array[String]): Unit = {
    println("Starting ClassTester at " + new Date())
    assert(args.size == 6 || args.size == 7)
    val cut = args(0)

    val seedBase = args(2).toInt
    val maxSuffixGenTries = args(3).toInt
    val callClinit = args(5).toBoolean
    val selectedCUTMethods: Option[ArrayList[String]] = if (args.size == 7) Some(readMethods(args(6))) else None
    if (selectedCUTMethods.isDefined) println("Focusing on " + selectedCUTMethods.get.size + " CUT methods")
    else println("No specific CUT methods selected")
    config = new Config(cut, seedBase, maxSuffixGenTries, selectedCUTMethods, new File("/tmp/"), callClinit)

    val resultFileName = args(4)
    config.addCheckerListener(new ResultFileCheckerListener(resultFileName))

    val envTypes = new ArrayList[String]
    envTypes.add("java.lang.Object")
    Util.addEnvTypes(args(1), envTypes, this.getClass.getClassLoader)

    stats = new Stats
    stats.timer.start("all")

    val typeProvider = new TypeManager(config.cut, envTypes, getClass.getClassLoader, new Random(seedBase))

    val seqMgr = new SequenceManager(new SequenceExecutor(stats, config), config, this)

    val global = new GlobalState(config, typeProvider, seqMgr, stats, new Random(seedBase), this)

    // Deadlock Detection
    val dlMonitor = new DeadlockMonitor(config, global)
    dlMonitor.setDaemon(true)
    dlMonitor.start

    // Initialize Potential CFPs
    stats.timer.start("pot_cfp")
    val cutMethods = new ClassReader(Class.forName(cut, true, getClass.getClassLoader)).readMethodAtoms
    println("CUTMethods Size - " + cutMethods.size)
    val potentialCFPs = new PotentialCFPs();
    potentialCFPs.writePotentialCFPs(cutMethods.mkString("@"))
    //potentialCFPs.writePotentialCFPs("java16.lang.StringBuffer.insert(int,java.lang.CharSequence)@java16.lang.StringBuffer.deleteCharAt(int)")
    stats.timer.stop("pot_cfp")

    var seed = seedBase;

    // Get next CFP from prioritizer and run test
    val nextCFP = new NextCFP();
    while (true) {
      stats.timer.start("next_cfp")
      seed = nextCFP.writeNextCFP(concRunRepetitions, 2, 100) + seedBase;
      val nextCFPMethod1 = getMethodAtom(NextCFP.nextCFPMethod1, cutMethods)
      val nextCFPMethod2 = getMethodAtom(NextCFP.nextCFPMethod2, cutMethods)
      stats.timer.stop("next_cfp")
      if (nextCFPMethod1 != null && nextCFPMethod2 != null) {
        val cutMethodsToTest1 = Seq(nextCFPMethod1, nextCFPMethod2)
        val cutMethodsToTest2 = Seq(nextCFPMethod2, nextCFPMethod1)
        var random: Random = null
        if (seedRandomMap.contains(seed)) {
          random = seedRandomMap(seed)
        } else {
          random = new Random(seed)
          seedRandomMap.put(seed, random)
        }
        var typeProvider: TypeManager = null;
        if (seedTypeProviderMap.contains(seed)) {
          typeProvider = seedTypeProviderMap(seed)
        } else {
          typeProvider = new TypeManager(config.cut, envTypes, getClass.getClassLoader, random)
          seedTypeProviderMap.put(seed, typeProvider)
        }
        
        var cutCallsPerSeq = 2
        
        if((seed - seedBase) > 5) {
          cutCallsPerSeq = 5
        }
          
        val tester = new ClassTester(config, stats, getClass.getClassLoader, ".", envTypes, random, this, cutMethodsToTest1, cutMethodsToTest2, seed, seedPrefix, typeProvider, cutCallsPerSeq)
        println("Testing " + cut + " with seed " + seed)
        tester.run
      }
    }
    finalizeAndExit(false)
  }

  def finalizeAndExit(bugFound: Boolean) = {
    stats.timer.stop("all")
    stats.print
    stats.timer.print2
    stats.timer.print_final()
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

  private def getMethodAtom(methodName: String, cutMethods: Seq[MethodAtom]): MethodAtom = {
    for (m <- cutMethods) {
      if (m.toString.equals(methodName)) {
        return m
      }
    }
    return null
  }

}
