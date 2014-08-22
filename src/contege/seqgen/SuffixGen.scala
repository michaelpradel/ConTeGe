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
import scala.collection.immutable.ListMap

/**
 * Generates suffixes for a given prefix.
 */
class SuffixGen(prefix: Prefix, global: GlobalState, config: Config) {

	//val onlyFocusMethods = new ArrayList[MethodAtom]()
	val weightedCutMethods = new ArrayList[MethodAtom]()
	global.typeProvider.cutMethods.foreach(m => weightedCutMethods.add(m))

	// prioritize the focus methods by adding them *multiple* (user defined) times to methodsToChoose
	global.focusMethods match {
		case (Some(map)) => {
			global.typeProvider.cutMethods.foreach(method => {
				if (map.keys.contains(method.signature)) {
					for (i <- Range(0, map(method.signature))) {
						//onlyFocusMethods.add(method)
						weightedCutMethods.add(method)
					}
				}
			})
		}
		case None => //ignore
	}

	def nextSuffix(cutCalls: Int, maxSuffixLength: Int): Option[Suffix] = {
		var suffix = new Suffix(prefix, global)
		var currentCutCalls = 0

		var i = 0;
		while (currentCutCalls < cutCalls) {
			val task = new CallCutMethodTask(suffix, weightedCutMethods, global)
			task.run match {
				case Some(newSeq) => {
					suffix = newSeq
				}
				case None => {
					return None
				}
			}
			currentCutCalls += 1
			if (suffix.length > maxSuffixLength) {
				global.stats.tooLongSeqs.incr
				println("The suffix length longer than the allowed maximum")
				return None
			}

			i += 1
		}
		return Some(suffix)
	}

}
