package contege
import java.io.PrintStream

class ResultFileCheckerListener(fileName: String) extends CheckerListener {

  private val ps = new PrintStream(fileName)

  def appendResultMsg(s: String) = {
    ps.println(s)
  }

  def notifyDoneAndBugFound(testCode: String) = {
    ps.flush
    ps.close
  }

  def notifyDoneNoBug = {
    ps.flush
    ps.close
  }

  // ignore everything that is not result-related
  def updateNbGeneratedTests(nb: Long) = {}
  def appendStatusMsg(s: String) = {}

}