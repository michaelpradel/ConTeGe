package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.io.PrintStream
import java.io.File
import contege.seqgen.TypedParameter

class Config(val cut: String, // class under test
		            val seed: Int, val maxSuffixGenTries: Int,
		            val selectedCUTMethodsForSuffix: Option[ArrayList[String]],
		            val workingDir: File,
		            val callClinit: Boolean) {
	
	def maxPrefixes = {
		var result = maxSuffixGenTries / 20 // try to create 20 suffixes for each prefix
		if (result < 1) result = 1
		result
	}
	
	val shareOnlyCUTObject = false
	
	val useJPFFirst = false

	private var checkerListenersVar: List[CheckerListener] = Nil
	def addCheckerListener(l: CheckerListener) = {
	  checkerListenersVar = l :: checkerListenersVar
	}
	def checkerListeners = checkerListenersVar
}

class SubclassTesterConfig(cut: String,  // the subclass
                        val oracleClass: String,   // the class to use as an oracle, e.g. the superclass
                        val constructorMap: ConstructorMapping, // map from superclass constructors to semantically equivalent subclass constructors; superclass constructors w/o a mapping cannot be used to be create CUT instances
		                seed: Int,
		                maxSuffixGenTries: Int,
		                val maxTests: Int,
		                val concurrentMode: Boolean,
		                selectedCUTMethods: Option[ArrayList[String]],
		                workingDir: File,
		                val compareOutputVectors: Boolean,
		                val stopOnOutputDiff: Boolean) extends Config(cut, seed, maxSuffixGenTries, selectedCUTMethods, workingDir, true) {
}

class PathTesterConfig (cut: String, val targetCutMethod: String, val targetMethodParameters: Set[List[Option[TypedParameter]]], seed: Int, val maxTests: Int, workingDir: File) extends Config(cut, seed, 0, None, workingDir, false) {
    
}