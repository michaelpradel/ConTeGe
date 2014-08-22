package contege.seqexec.jpf
import contege.seqgen.AbstractCallSequence
import contege.seqgen.Suffix
import contege.seqgen.Prefix
import contege.Stats
import java.util.ArrayList
import contege.seqexec._
import contege.GlobalState
import scala.collection.mutable.Set
import scala.collection.JavaConversions._
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationTargetException
import java.io.PrintStream
import contege.seqexec.reflective.SequenceExecutor

/*
 * Assumed to be used only for thread-safety testing (not for subclass testing).
 * Otherwise, must propagate createOutputVectors switch to JPF executor.
 */
class JPFFirstSequenceExecutor(globalState: GlobalState, putJarPath: String) {

    private val reflectiveExecutor = new SequenceExecutor(globalState.stats, globalState.config)
    private val jpfExecutor = new RobustConcJPFExecutor(globalState, putJarPath, false)
  	
    // TODO : JPF executor should return not only strings but exception that can be compared
    
	def executeConcurrently(prefix: Prefix,
							suffix1: Suffix,
							suffix2: Suffix): Set[String] = {
	    // try JPF first
	    val (errorsOption, _) = jpfExecutor.executeConcurrently(prefix, suffix1, suffix2, TestPrettyPrinter.NoOutputVectors)
	    if (errorsOption.isDefined) {
	        // JPF run was successful
	        return errorsOption.get
	    } else {
	    	// fall back to normal concurrent execution if JPF run is inconclusive (timeout or JPF crash)
	        val throwables = reflectiveExecutor.executeConcurrently(prefix, suffix1, suffix2)
	        
	        val errors = throwables.map(t => {
                val realException = if (t.isInstanceOf[InvocationTargetException]) t.asInstanceOf[InvocationTargetException].getCause
                else t

                val baos = new ByteArrayOutputStream
                realException.printStackTrace(new PrintStream(baos))
                baos.toString
	        })
	        
	        val result = Set[String]()
	        result.addAll(errors)
	        result
	    }
	}
    
    def execute(prefix: Prefix) = {
        executeConcurrently(prefix, new Suffix(prefix, globalState), new Suffix(prefix, globalState))
    }
    
}