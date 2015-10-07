package contege.seqexec

import org.apache.commons.io.IOUtils
import contege.seqgen.Prefix
import java.io.PrintWriter
import java.io.File

class JavaSkeleton(var javaSkeleton : String, var className : String, var objectName : String) {
  
  private val skeletonStream = Class.forName("contege.TestSkeletonHelper").getResource("TestSkeletonSequential.noJava").openStream
  javaSkeleton = IOUtils.toString(skeletonStream)
  skeletonStream.close
  
  val testClass : StringBuffer = new StringBuffer();
  
  def javaCodeFor(prefix: Prefix) = {  
      testClass.append(javaSkeleton.replace("CLASSNAME", className).replace("PREFIX_TO_REPLACE", prefix.toString))
  }
  
  def writeJavaTest {
      val writer = new PrintWriter(new File("SeqTeGeTest.java" ))
      writer.write(testClass.toString())
      writer.close()
  }
}