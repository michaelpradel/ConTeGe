package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.io.PrintStream
import java.io.File
import java.io.BufferedOutputStream
import java.io.FileOutputStream

class Config(val workingDir: File) {
	
	def maxPrefixes = {
		var result = maxSuffixGenTries / 5 // try to create 10 suffixes for each prefix
		if (result < 1) result = 1
		result
	}

	val shareOnlyCUTObject = false

	val useJPFFirst = false

	var performanceSummaryWriter: FileWriter = _

	var debugLevel = 0
	var projectId = ""
	var cut = ""
	var host = ""
	var seed = 0
	var callClinit = false
	// TODO change to 20
	var maxSuffixGenTries = 10
	var selectedCUTMethods: Option[ArrayList[String]] = Some(new ArrayList[String])
	var cutCallsPerSeq = 0
	var maxSuffixLength = 0
	var maxStateChangersInPrefix = 0
	var chooseCUTCall = 0
	var suffixRuns = 0
	var nrThreads: Array[Int] = Array[Int]()
	var runningTimeMultipliers: Array[Double] = Array[Double]()
	var revisionV1 = ""
	var revisionV2 = ""
	var noisyEnvironment = false
	var targetThresholdQuiet = 0.01
	var acceptableThresholdQuiet = 0.02
	var targetThresholdNoisy = 0.03
	var acceptableThresholdNoisy = 0.05

	private var checkerListenersVar: List[CheckerListener] = Nil

	def setPerformanceSummaryWriter(filePath: String, fileName: String) {
		performanceSummaryWriter = new FileWriter(filePath, fileName)
	}

	private var performanceListenersVar: List[PerformanceListener] = Nil
	def addPerformanceListener(l: PerformanceListener) = {
		performanceListenersVar = l :: performanceListenersVar
	}
	def performanceListeners = performanceListenersVar
}
class SubclassTesterConfig(cut: String, // the subclass
	val oracleClass: String, // the class to use as an oracle, e.g. the superclass
	val constructorMap: ConstructorMapping, // map from superclass constructors to semantically equivalent subclass constructors; superclass constructors w/o a mapping cannot be used to be create CUT instances
	seed: Int,
	maxSuffixGenTries: Int,
	val maxTests: Int,
	val concurrentMode: Boolean,
	selectedCUTMethods: Option[ArrayList[String]],
	workingDir: File,
	val compareOutputVectors: Boolean,
	val stopOnOutputDiff: Boolean, val summaryFile: String) extends Config(workingDir) {
}

class FileWriter(filePath: String, fileName: String) {

	private val directory = new File(filePath)
	if (!directory.exists) {
		directory.mkdirs
	}

	private var file = filePath + "/" + fileName

	private val ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(file, true)))

	def appendResultMsgWONewline(s: String) = {
		ps.print(s)
	}

	def appendResultMsg(s: String) = {
		ps.println(s)
	}

	def appendConfig(config: Config, focusMethods: scala.collection.immutable.Map[String, Int]) {
		ps.println("host: " + config.host)
		ps.println("revision_v1:" + config.revisionV1)
		ps.println("revision_v2:" + config.revisionV2)
		ps.println("cut_calls_per_seq:" + config.cutCallsPerSeq)
		ps.println("max_suffix_length:" + config.maxSuffixLength)
		ps.println("max_state_changes_in_prefix:" + config.maxStateChangersInPrefix)
		ps.println("choose_only_cut_methods:" + config.chooseCUTCall)
		ps.println("suffix_runs:" + config.suffixRuns)
		ps.println("nr_threads:" + config.nrThreads.mkString(","))
		ps.print("focus_methods:")
		focusMethods.map { case (method, priority) => method + "[" + priority + "];" }.foreach { method => ps.print(method) }
		ps.println()
	}

	def write = {
		ps.flush
		ps.close
	}

}
