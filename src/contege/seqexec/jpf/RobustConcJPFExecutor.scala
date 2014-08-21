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
     * Returns the errors obtained (empty set means no errors),
     * or None if the result is unknown (e.g. because of a timeout or
     * because the test fails sequentially).
     */
    def executeConcurrently(prefix: Prefix,
							suffix1: Suffix,
							suffix2: Suffix): Option[Set[String]] = {
        // run linearizations sequentially
        val interleavings = new SequentialInterleavings(prefix, suffix1, suffix2)
		var failedSequentially = false
		var interleaving = interleavings.nextInterleaving
		while (interleaving.isDefined) {
		    global.seqMgr.seqExecutor.execute(interleaving.get) match {
				case Some(interleavingException) => {
				    println("Sequential execution gives exception!")
				    return None
				}
				case None => // ignore, seq. execution succeeds
			}
		    interleaving = interleavings.nextInterleaving
		}
        
        // run with timeout
        val execThread = new Thread() {
            @volatile var runFinished = false
            @volatile var result: Option[Set[String]] = null
            
            override def run = {
                // redirect stderr and stdout to buffer (afterwards, write everything to stdout)
            	val oldStdOut = System.out
                val oldStdErr = System.err
            	
                val buffer = new ByteArrayOutputStream
            	System.setOut(new PrintStream(buffer))
            	System.setErr(new PrintStream(buffer))
		
            	try {
            		result = jpfExecutor.executeConcurrently(prefix, suffix1, suffix2)    
            	} catch {
            		case t: Throwable => { // JPF crashed - result of execution remains unknown
            			result = None
            		}
            	}
		
            	// reset stderr and stdout
            	System.setOut(oldStdOut)
            	System.setErr(oldStdErr)
            	
            	val bufferContent = buffer.toString
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
			return None   
		}
        execThread.result match {
            case Some(errorMsgs) => {
            	if (errorMsgs.isEmpty) return Some(Set[String]())
                val filteredErrorMsgs = errorMsgs.filter(m => !m.contains("java.lang.UnsatisfiedLinkError: cannot find native"))
                if (filteredErrorMsgs.isEmpty) return None // only errors are due to a JPF limitation - inconclusive
                else return Some(filteredErrorMsgs)
                
            }
            case None => return None  // JPF crashed, inconclusive
        }
    }
    
}