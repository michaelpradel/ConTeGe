package contege.seqexec
import contege.seqgen.Suffix
import contege.seqgen.Prefix
import contege.Stats
import contege.Finalizer
import contege.Config

abstract class TSOracle(finalizer: Finalizer, stats: Stats, config: Config) {

	def analyzeTest(prefix: Prefix, suffix1: Suffix, suffix2: Suffix)
    
}