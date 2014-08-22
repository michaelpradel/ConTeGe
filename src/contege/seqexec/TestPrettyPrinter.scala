package contege.seqexec
import contege.seqgen.Suffix
import contege.seqgen.Prefix
import org.apache.commons.io.IOUtils

/**
 * Prints tests as (supposedly) pretty Java source code.
 */
object TestPrettyPrinter {

    private val skeletonStream = Class.forName("contege.TestSkeletonHelper").getResource("TestSkeleton.noJava").openStream
    private val javaSkeleton = IOUtils.toString(skeletonStream)
    skeletonStream.close
    
    private val skeletonWithOutputVectorStream = Class.forName("contege.TestSkeletonHelper").getResource("TestSkeletonWithOutputVector.noJava").openStream
    private val javaSkeletonWithOutputVector = IOUtils.toString(skeletonWithOutputVectorStream)
    skeletonWithOutputVectorStream.close

    class OutputConfig
    case object NoOutputVectors extends OutputConfig
    case object SuffixOutputVectors extends OutputConfig
    case object FullOutputVectors extends OutputConfig
    
    def javaCodeFor(prefix: Prefix, suffix1: Suffix, suffix2: Suffix, testName: String, outputConfig: OutputConfig) = {
        outputConfig match {
            case NoOutputVectors => {
                javaSkeleton.replaceAll("CLASSNAME", testName)
	              .replace("PREFIX", prefix.toString)
	              .replace("SUFFIX1", suffix1.toString)
	              .replace("SUFFIX2", suffix2.toString)
            }
            case SuffixOutputVectors => {
                javaSkeletonWithOutputVector.replaceAll("CLASSNAME", testName)
	              .replace("PREFIX", prefix.toString)
	              .replace("SUFFIX1", suffix1.toStringWithOutputVector)
	              .replace("SUFFIX2", suffix2.toStringWithOutputVector)
            }
            case FullOutputVectors => {
                javaSkeletonWithOutputVector.replaceAll("CLASSNAME", testName)
	              .replace("PREFIX", prefix.toStringWithOutputVector)
	              .replace("SUFFIX1", suffix1.toStringWithOutputVector)
	              .replace("SUFFIX2", suffix2.toStringWithOutputVector)
            }
        }
    }

}
