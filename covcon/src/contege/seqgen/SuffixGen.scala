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

  def nextSuffix(cutCalls: Int, cutMethodsToTest: Seq[MethodAtom]): Option[Suffix] = {

    var suffix = new Suffix(prefix, global)
    var currentCutCalls = 0
    while (currentCutCalls < cutCalls) {
      Counter.current = currentCutCalls % 2
      val task = new CallCutMethodTask(suffix, cutMethodsToTest, global)
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
    return Some(suffix)
  }

}
