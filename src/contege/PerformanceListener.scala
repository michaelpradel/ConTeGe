package contege
import java.io.PrintWriter

abstract class PerformanceListener {
  
  def updateNbGeneratedTests(nb: Long)

  /**
   * Some output of the checker that may be of interest
   * to users.
   * (Not for debugging output,
   * not for reporting details about a bug found.)
   */
  def appendStatusMsg(s: String)
  
  /**
   * Info about a performance measurement.
   */
  def appendResultMsg(s: String)

  /**
   * Called when the test runs are done
   */
  def notifyDone
}
