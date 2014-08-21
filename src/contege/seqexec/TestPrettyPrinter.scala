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
    
    def javaCodeFor(prefix: Prefix, suffix1: Suffix, suffix2: Suffix, testName: String) = {
        javaSkeleton.replaceAll("CLASSNAME", testName)
          .replace("PREFIX", prefix.toString)
          .replace("SUFFIX1", suffix1.toString)
              .replace("SUFFIX2", suffix2.toString)
    }

}
