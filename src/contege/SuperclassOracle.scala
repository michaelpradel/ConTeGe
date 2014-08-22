package contege

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException

import contege.seqexec.reflective.SequenceExecutor
import contege.seqexec.OutputVector
import contege.seqgen.AbstractCallSequence
import contege.seqgen.Prefix
import contege.seqgen.Suffix

class SuperclassOracle(config: SubclassTesterConfig, seqExecutor: SequenceExecutor, finalizer: Finalizer) {

    private val oracleClass = config.asInstanceOf[SubclassTesterConfig].oracleClass
    private val concMode = config.asInstanceOf[SubclassTesterConfig].concurrentMode
    
    /**
     * Checks whether a sequence that fails with Sub also fails with Super.
     * If it does, reports an error and terminates checking.
     */
    def checkFailingSequence(seq: AbstractCallSequence[_], failure: Throwable) = {
        // only check against superclass if we're in sequential mode -- in conc. mode we are looking for bugs involving two suffixes
        if (!concMode) {
	        // ignore sequences where last call is constructor call to CUT --> warnings not useful because programmer knows which class (s)he deals with
	        // ignore sequences where last call is not a CUT call --> we look for bugs in CUT
	        if (seq.endsWithCUTCall && !seq.endsWithCUTInstantiation) {
	            // create copy using superclass oracle as receiver
	            val oracleSeq = copyWithOracleReceiver(seq)
	            assert(oracleSeq.length == seq.length)
	            
			    seqExecutor.execute(oracleSeq) match {
			        case Some(_) => // ignore -- also ignores case where oracle class throws another exception -- ensures tt no false warnings
			        case None => {
			            // write to result listener(s)
			            config.checkerListeners.foreach(_.appendResultMsg("@@@ Call sequence fails for subclass but succeeds for superclass @@@"))
				        printSequence(oracleSeq)
			            reportExceptionFromExecutor(failure)    
	
			            // exit: we found what we are looking for
			            finalizer.finalizeAndExit(true)
			        }
			    }    
	        }    
        }
    }
    
    /**
     * Checks whether a sequence that succeeds for Sub gives the
     * same output vector for Super.
     * If it doesn't, reports an error and terminates checking.
     */
    def checkSucceedingSequence(seq: AbstractCallSequence[_], expectedOutput: OutputVector) = {
        if (!concMode) {
            // ignore sequences where last call is constructor call to CUT --> warnings not useful because programmer knows which class (s)he deals with
	        // ignore sequences where last call is not a CUT call --> we look for bugs in CUT
	        if (seq.endsWithCUTCall && !seq.endsWithCUTInstantiation) {
	            // create copy using superclass oracle as receiver
	            val oracleSeq = copyWithOracleReceiver(seq)
	            assert(oracleSeq.length == seq.length)
	            
			    val (failureOpt, outputVector) = seqExecutor.executeWithOutputVector(oracleSeq)
			    if (!failureOpt.isDefined) {  // if fails for Super, not interesting for substitutability checking --> ignore
			    	if (expectedOutput != outputVector) {
			    	    if (config.stopOnOutputDiff) {
				    	    config.checkerListeners.foreach(_.appendResultMsg("@@@ Call sequence gives different output vectors for sub- and superclass @@@"))
				    	    config.checkerListeners.foreach(_.appendResultMsg(seq.toString))
				    	    config.checkerListeners.foreach(_.appendResultMsg(">> Super's output vector:\n"+outputVector+"\n"))
				    	    config.checkerListeners.foreach(_.appendResultMsg(">> Sub's output vector:\n"+expectedOutput+"\n"))
				    	    
				    	    // exit: we found what we are looking for
				            finalizer.finalizeAndExit(true)    
			    	    } else {
			    	        println("@@@ Call sequence gives different output vectors for sub- and superclass @@@")
				    	    println(seq.toString)
				    	    println(">> Super's output vector:\n"+outputVector+"\n")
				    	    println(">> Sub's output vector:\n"+expectedOutput+"\n")
			    	    }
			    	}
			    }
	        }
        }
    }
    
    private def copyWithOracleReceiver(seq: AbstractCallSequence[_]) = {
        if (seq.isInstanceOf[Prefix]) {
            val prefix = seq.asInstanceOf[Prefix]
            prefix.copyWithOracleClassReceiver(oracleClass)    
        } else {
        	val suffix = seq.asInstanceOf[Suffix]
	        val oraclePrefix = suffix.prefix.copyWithOracleClassReceiver(oracleClass)
	        suffix.copyWithOracleClassReceiver(oracleClass, oraclePrefix)	
        }
    }
    
    private def printSequence(seq: AbstractCallSequence[_]) = {
        if (seq.isInstanceOf[Prefix]) config.checkerListeners.foreach(_.appendResultMsg(seq.toString))
        else config.checkerListeners.foreach(_.appendResultMsg("Prefix:\n"+seq.asInstanceOf[Suffix].prefix.toString+"Suffix:\n"+seq.toString))
    }
    
	private def reportExceptionFromExecutor(t: Throwable) = {
	    val realException = if (t.isInstanceOf[InvocationTargetException]) t.asInstanceOf[InvocationTargetException].getCause
							else t
		if (!realException.isInstanceOf[OutOfMemoryError]) { // ignore memory problems because they may be implementation-dependent
		    // write stack trace
		    val baos = new ByteArrayOutputStream
			realException.printStackTrace(new PrintStream(baos))
			config.checkerListeners.foreach(_.appendResultMsg(baos.toString))
			
			// write message
			if (realException.getMessage != null) config.checkerListeners.foreach(_.appendResultMsg(realException.getMessage+"\n"))
		}
	}
    
}
