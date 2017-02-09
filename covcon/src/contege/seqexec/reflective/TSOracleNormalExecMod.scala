package contege.seqexec.reflective

import contege.seqexec._
import contege.seqgen.Suffix
import contege.seqgen.Prefix
import scala.collection.JavaConversions._
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationTargetException
import java.io.PrintStream
import contege.Stats
import contege.SequentialInterleavings
import contege.Config
import contege.Finalizer
import cfp.CFPDetection
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class TSOracleNormalExecMod(finalizer: Finalizer, concRunRepetitions: Int,
  stats: Stats, executor: SequenceExecutor,
  config: Config) extends TSOracle(finalizer, stats, config) {

  override def analyzeTest(prefix: Prefix, suffix1: Suffix, suffix2: Suffix) = {
    println("==== Starting Java scheduler-based execution ====")

    for (rep <- 1 to concRunRepetitions) {

      stats.timer.start("conc_exec")

      val concExecExceptions = executor.executeConcurrently(prefix, suffix1, suffix2)

      stats.timer.stop("conc_exec")

      if (!concExecExceptions.isEmpty) { // one or two exceptions during concurrent execution
        // try sequential interleavings to see if we can trigger the same exception that way
        stats.timer.start("interleavings")
        val interleavings = new SequentialInterleavings(prefix, suffix1, suffix2)
        var failedSequentially = false
        var interleaving = interleavings.nextInterleaving
        while (!failedSequentially && interleaving.isDefined) {
          stats.sequentialInterleavings.incr
          executor.execute(interleaving.get) match {
            case Some(interleavingException) => {
              if (concExecExceptions.exists(ce => sameExceptionKind(ce, interleavingException))) failedSequentially = true
            }
            case None => // ignore
          }
          interleaving = interleavings.nextInterleaving
        }
        stats.timer.stop("interleavings")
        if (!failedSequentially) {

          val exceptionFound: StringBuffer = new StringBuffer;

          concExecExceptions.foreach(exceptionFromConcurrent => {
            val realException = if (exceptionFromConcurrent.isInstanceOf[InvocationTargetException]) exceptionFromConcurrent.asInstanceOf[InvocationTargetException].getCause
            else exceptionFromConcurrent

            val baos = new ByteArrayOutputStream
            realException.printStackTrace(new PrintStream(baos))
            if (!(baos.toString().contains("contege.seqexec.reflective.TimeoutException"))) {
              exceptionFound.append("Exception Found : ")
              exceptionFound.append(baos.toString)
              exceptionFound.append("\n")
              exceptionFound.append(realException.getMessage)
              exceptionFound.append("\n")
            }
          })

          if (exceptionFound.toString().length() > 0) {

            config.checkerListeners.foreach(_.appendResultMsg("\n==== Found a thread safety violation! ===="))
            config.checkerListeners.foreach(_.appendResultMsg("Sequential prefix:\n" + prefix + "\nConcurrent suffixes:\n"))
            config.checkerListeners.foreach(_.appendResultMsg(suffix1.toString))
            config.checkerListeners.foreach(_.appendResultMsg("vs.\n"))
            config.checkerListeners.foreach(_.appendResultMsg(suffix2.toString))
            config.checkerListeners.foreach(_.appendResultMsg(exceptionFound.toString))
            finalizer.finalizeAndExit(true)
          }
        }
      }
    }
  }

  private def sameExceptionKind(outcome1: Throwable, outcome2: Throwable): Boolean = {
    if (outcome1.isInstanceOf[InvocationTargetException] && outcome2.isInstanceOf[InvocationTargetException]) {
      val realMsg1 = outcome1.asInstanceOf[InvocationTargetException].getCause
      val realMsg2 = outcome2.asInstanceOf[InvocationTargetException].getCause
      return realMsg1.getClass.getName == realMsg2.getClass.getName
    } else {
      outcome1.getClass.getName == outcome2.getClass.getName
    }

  }
}