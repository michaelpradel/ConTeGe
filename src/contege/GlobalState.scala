package contege
import contege.seqexec.reflective.SequenceManager
import contege.seqgen.TypeManager

class GlobalState(val config: Config,
	val typeProvider: TypeManager,
	val seqMgr: SequenceManager,
	val stats: Stats,
	val random: Random,
	val finalizer: Finalizer,
	val classLoaderV1: CustomClassLoader,
	val classLoaderV2: CustomClassLoader,
	val focusMethods: Option[Map[String, Int]]) {

	def debug(message: String, level: Int) {
		if (level <= config.debugLevel) {
			println(message)
		}
	}
}

