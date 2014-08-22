package contege.seqexec.jpf

import java.io.ByteArrayOutputStream
import java.io.PrintStream

import scala.annotation.elidable.ASSERTION
import scala.collection.mutable.Set

import contege.seqexec._
import contege.seqgen.Prefix
import contege.seqgen.Suffix
import contege.GlobalState
import contege.SequentialInterleavings

/**
 * Robust way to model check concurrent tests.
 * Before invoking JPF, tries to run all linearizations sequentially,
 * to avoid running JPF if seq. execution already fails.
 * Runs JPF with a timeout and reports "don't know" (None) if JPF doesn't
 * terminate before the timeout.
 * 
 */
class RobustConcJPFExecutor(global: GlobalState, putJarPath: String, returnOutputVectors: Boolean) {

    private val timeout = 10000
    
    private val jpfExecutor = new JPFSequenceExecutor(global.config, putJarPath)
    
    /**
     * First part of returned pair: The errors obtained (empty set means no errors),
     * or None if the result is unknown (e.g. because of a timeout or
     * because the test fails sequentially).
     * Second part of returned pair: Set of output vectors, or None if the run was inconclusive or
     * if returnOutputVectors was set to false.
     */
    def executeConcurrently(prefix: Prefix,
							suffix1: Suffix,
							suffix2: Suffix,
							outputConfig: TestPrettyPrinter.OutputConfig): (Option[Set[String]], Option[Set[OutputVector]]) = {
        // run linearizations sequentially
        val interleavings = new SequentialInterleavings(prefix, suffix1, suffix2)
		var failedSequentially = false
		var interleaving = interleavings.nextInterleaving
		while (interleaving.isDefined) {
		    global.seqMgr.seqExecutor.execute(interleaving.get) match {
				case Some(interleavingException) => {
				    println("Sequential execution gives exception!")
				    return (None, None)
				}
				case None => // ignore, seq. execution succeeds
			}
		    interleaving = interleavings.nextInterleaving
		}
        
        // run with timeout
        val execThread = new Thread() {
            @volatile var runFinished = false
            @volatile var result: Option[Set[String]] = null
            @volatile var outputVectorsOpt: Option[Set[OutputVector]] = null
            
            override def run = {
                // redirect stderr and stdout to buffer (afterwards, write everything to stdout)
            	val oldStdOut = System.out
                val oldStdErr = System.err
            	
                val buffer = new ByteArrayOutputStream
            	System.setOut(new PrintStream(buffer))
            	System.setErr(new PrintStream(buffer))
		
            	try {
            		result = jpfExecutor.executeConcurrently(prefix, suffix1, suffix2, outputConfig)    
            	} catch {
            		case t: Throwable => { // JPF crashed - result of execution remains unknown
            			result = None
            		}
            	}
		
            	// reset stderr and stdout
            	System.setOut(oldStdOut)
            	System.setErr(oldStdErr)
            	
            	// if enabled, get output vectors from buffer
            	val bufferContent = buffer.toString
            	outputVectorsOpt = if (returnOutputVectors) {
	            	val outputVectors = Set[OutputVector]()
            	    val lines = bufferContent.split("\n")
	            	var currentOutputVectorContent: Option[StringBuilder] = None
            	    lines.foreach(line => {
	            	    if (line == "OUTPUTVECTOR_BEGIN") {
	            	        assert(!currentOutputVectorContent.isDefined)
	            	        currentOutputVectorContent = Some(new StringBuilder)
	            	    } else if (line == "OUTPUTVECTOR_END") {
	            	        assert(currentOutputVectorContent.isDefined)
	            	        if (currentOutputVectorContent.get.size > 0) currentOutputVectorContent.get.deleteCharAt(currentOutputVectorContent.get.size-1) // remove newline at end
	            	        val outputVector = OutputVector.fromString(currentOutputVectorContent.get.toString)
	            	        outputVectors.add(outputVector)
	            	        currentOutputVectorContent = None
	            	    } else if (currentOutputVectorContent.isDefined) {
	            	        currentOutputVectorContent.get.append(line+"\n")
	            	    }
	            	})
	            	Some(outputVectors)
            	} else None
            	
            	// write buffer to stdout
            	println(bufferContent)

            	assert(result != null)
                runFinished = true
            }
        }
		execThread.start
		execThread.join(timeout)
		if (!execThread.runFinished) {
		    println("Timeout when running concurrent execution with JPF")
		    println(prefix+"With suffix1:\n"+suffix1+".. and suffix2:\n"+suffix2)
			return (None, None)   
		}
        execThread.result match {
            case Some(errorMsgs) => {
            	if (errorMsgs.isEmpty) return (Some(Set[String]()), execThread.outputVectorsOpt)
                val filteredErrorMsgs = errorMsgs.filter(m => !m.contains("java.lang.UnsatisfiedLinkError: cannot find native"))
                if (filteredErrorMsgs.isEmpty) return (None, None) // only errors are due to a JPF limitation - inconclusive
                else return (Some(filteredErrorMsgs), execThread.outputVectorsOpt)
                
            }
            case None => return (None, None)  // JPF crashed, inconclusive
        }
    }
    
}