package contege.seqexec.jpf

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.{ List => JList }
import java.util.ArrayList
import java.util.Collections
import java.io.File
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.FileReader
import contege._
import contege.seqexec._
import contege.seqgen._
import com.sun.tools.javac.{ Main => Javac }
import org.apache.commons.io.FileUtils
import gov.nasa.jpf.JPF
import gov.nasa.jpf.Error
import java.io.PrintStream
import org.apache.commons.io.IOUtils

/**
 * Calls JPF for concurrently executing tests.
 * Sequential execution is done reflectively by SequenceExecutor.
 */
class JPFSequenceExecutor(config: Config, putJarPath: String) {

    private val workingDir = new File(config.workingDir, "contege-jpf-bridge")
    if (!workingDir.exists) workingDir.mkdir

    private var execCtr = 0

    private val currentClassPath = System.getProperty("java.class.path")+":"+putJarPath+":"+workingDir.getAbsolutePath

    def executeConcurrently(prefix: Prefix,
        suffix1: Suffix,
        suffix2: Suffix,
        outputConfig: TestPrettyPrinter.OutputConfig): Option[Set[String]] = {
        execCtr += 1
        cleanWorkingDir()

        // generate .java file
        val javaFile = generateJavaFile(prefix, suffix1, suffix2, outputConfig)

        // compile
        if (!compile(javaFile)) return None

        // invoke JPF and interpret outcome
        val errors = runJPF(testName)
        Some(errors.map(_.getDetails))
    }

    private def testName = "Test" + config.seed + "_" + execCtr

    private def cleanWorkingDir() = {
        FileUtils.cleanDirectory(workingDir)
    }

    private def generateJavaFile(prefix: Prefix, suffix1: Suffix, suffix2: Suffix, outputConfig: TestPrettyPrinter.OutputConfig) = {
        val javaCode = TestPrettyPrinter.javaCodeFor(prefix, suffix1, suffix2, testName, outputConfig)
        
        val javaFile = new File(workingDir, testName + ".java")

        val javaFileWriter = new PrintWriter(javaFile)
        javaFileWriter.println(javaCode)
        javaFileWriter.close()

        javaFile
    }

    private def compile(javaFile: File): Boolean = {
        val javacArgs = "-cp "+currentClassPath+" "+javaFile.getAbsolutePath
        val javacRet = Javac.compile(javacArgs.split(" "))
        if (javacRet != 0) {
            println("Error during compilation:")
            val r = new BufferedReader(new FileReader(javaFile))
            var line = r.readLine
            while (line != null) {
                println(line)
                line = r.readLine
            }
            r.close
            println("------------")
            return false
        }
        true
    }

    private def runJPF(testName: String) = {
        val errors = Set[Error]()

        val conf = JPF.createConfig(("+classpath=" + workingDir.getAbsolutePath + ":" + currentClassPath + " " + testName).split(" "))
        val jpf = new JPF(conf)
        jpf.run()
        if (jpf.foundErrors) {
            println(">>> JPF has found errors: " + jpf.getSearchErrors.size + " <<<")
            errors.addAll(jpf.getSearchErrors)
        }
        errors
    }

}

object JPFSequenceExecutor extends App {
    val config = new Config("cut", 1, 20, None, new File("/home/m/temp/"), true)
    val jpf = new JPFSequenceExecutor(config, "/home/m/java/jAlbum/JAlbum.jar")
    
    println(jpf.workingDir.getAbsolutePath())
    
    jpf.compile(new File(jpf.workingDir+"/Test.java"))
    val errors = jpf.runJPF("Test")
    
    println(errors)
    
}