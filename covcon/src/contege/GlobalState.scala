package contege
import contege.seqexec.reflective.SequenceManager
import contege.seqgen.TypeManager

class GlobalState(val config: Config,
                  val typeProvider: TypeManager,
                  val seqMgr: SequenceManager,
                  val stats: Stats,
                  val random: Random,
                  val finalizer: Finalizer) {

}
