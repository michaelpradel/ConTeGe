package contege.seqgen

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import contege.ClassReader
import contege.Random
import contege.Atom
import contege.ConstructorAtom
import contege.MethodAtom
import contege.seqexec._
import contege._

/**
 * Generates suffixes for a given prefix.
 */
class SuffixGen(prefix: Prefix, val maxSuffixLength: Int,
                global: GlobalState) {
	
	val cutMethods = if (global.config.selectedCUTMethods.isDefined) {
		val selected = global.config.selectedCUTMethods.get
		global.typeProvider.cutMethods.filter(m => selected.contains(m.signature))
	} else global.typeProvider.cutMethods

	println("CUT methods:\n"+cutMethods.mkString("\n"))
	
	def nextSuffix(cutCalls: Int): Option[Suffix] = {
		var suffix = new Suffix(prefix, global)
		var currentCutCalls = 0
		while (currentCutCalls < cutCalls) {
			val task = new CallCutMethodTask(suffix, cutMethods, global)
			task.run match {
				case Some(newSeq) => suffix = newSeq
				case None => return None
			}	
			currentCutCalls += 1
			if (suffix.length > maxSuffixLength) {
				global.stats.tooLongSeqs.incr
				return None
			}
		}
		println("New prefix-suffix pair:\n"+prefix+"vvvvvvvvv\n"+suffix)
		return Some(suffix)
	}
		
}
