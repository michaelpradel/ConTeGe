package contege.seqexec.jpf

import contege.seqexec._
import contege.Stats
import contege.Config
import contege.Finalizer
import contege.seqgen.Suffix
import contege.seqgen.Prefix
import contege.SequentialInterleavings
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationTargetException
import contege.seqexec.reflective.SequenceExecutor

class TSOracleJPFFirst(finalizer: Finalizer, stats: Stats, config: Config,
                       normalExecutor: SequenceExecutor, jpfFirstExecutor: JPFFirstSequenceExecutor)
extends TSOracle(finalizer: Finalizer, stats: Stats, config: Config) {
    
    override def analyzeTest(prefix: Prefix, suffix1: Suffix, suffix2: Suffix) = {
    	        println("==== Starting JPF-first scheduler-based execution ====")
        
            stats.timer.start("conc_exec")
            val concExecErrors = jpfFirstExecutor.executeConcurrently(prefix, suffix1, suffix2)
            stats.timer.stop("conc_exec")
            if (!concExecErrors.isEmpty) { // one or two exceptions during concurrent execution
                // try sequential interleavings to see if we can trigger the same exception that way
                stats.timer.start("interleavings")
                val interleavings = new SequentialInterleavings(prefix, suffix1, suffix2)
                var failedSequentially = false
                var interleaving = interleavings.nextInterleaving
                while (!failedSequentially && interleaving.isDefined) {
                    stats.sequentialInterleavings.incr
                    normalExecutor.execute(interleaving.get) match {
                        // TODO should compare type of error - currently, only check if there's an error
                        case Some(interleavingException) => failedSequentially = true
                        case None => // ignore
                    }
                    interleaving = interleavings.nextInterleaving
                }
                if (!failedSequentially) {
                    println("\nTSOracleJPFFirst: Seems we found a bug. Will try JPF linearizations to be sure.")
                    
                    // to avoid false warnings due to JPF-specific errors, test all linearizations in JPF
                    var failedSeqInJPF = false
                	val interleavingsForJPF = new SequentialInterleavings(prefix, suffix1, suffix2)
	                var interleavingForJPF = interleavingsForJPF.nextInterleaving
	                while (!failedSeqInJPF && interleavingForJPF.isDefined) {
	                    val interleavingJPFErrors = jpfFirstExecutor.execute(interleavingForJPF.get)
	                    if (!interleavingJPFErrors.isEmpty) failedSeqInJPF = true
	                    interleavingForJPF = interleavingsForJPF.nextInterleaving
	                }
                    
                    println("TSOracleJPFFirst: failedSeqInJPF="+failedSeqInJPF)
                    
                    if (!failedSeqInJPF) {
	                    config.checkerListeners.foreach(_.appendResultMsg("\n==== Found a thread safety violation! ===="))
	                    config.checkerListeners.foreach(_.appendResultMsg("Sequential prefix:\n" + prefix + "\nConcurrent suffixes:\n"))
	                    config.checkerListeners.foreach(_.appendResultMsg(suffix1.toString))
	                    config.checkerListeners.foreach(_.appendResultMsg("vs.\n"))
	                    config.checkerListeners.foreach(_.appendResultMsg(suffix2.toString))
	                    concExecErrors.foreach(e => {
	                        config.checkerListeners.foreach(_.appendResultMsg(e))
	                    })
	                    finalizer.finalizeAndExit(true)    
                    }
                }
                stats.timer.stop("interleavings")

            }
    }

}