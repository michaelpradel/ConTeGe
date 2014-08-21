package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.io.PrintStream
import java.io.File

class Config(val cut: String, // class under test
		            val seed: Int, val maxSuffixGenTries: Int,
		            val selectedCUTMethods: Option[ArrayList[String]],
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
